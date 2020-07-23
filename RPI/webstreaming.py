from detectors.motion_detection.singlemotiondetector import SingleMotionDetector
from detectors.face_detection.opencv_detection import OpenCVDetector
from cryptoutils import HMACSHA256, AESCipher
from tokenList import TokenList
from utils.helper import StringHelper
from utils.param import Param

import threading
import argparse
import datetime
import time
from random import SystemRandom
import json
import re

import imutils
from imutils.video import VideoStream
import cv2
from flask import Response, Flask, render_template, request, abort
import firebase_admin
from firebase_admin import credentials, messaging
from gpiozero import Button

# Multithreading vars
outputFrame = None
stream = True
tokens = TokenList()
outputFrame_lock = threading.Lock()

notification_flag = Param.NOTIFICATION_NONE
notification_lock = threading.Lock()


# GPIO
doorbell_button = Button(23)

# Flask 
app = Flask(__name__)

# Static
try:
    with open("config.json") as f:
        settings = json.load(f)
        seed = settings['seed']
except:
    print("Seed is not available. Try running init script again.", flush=True)
    exit(0)

# Don't forget to remove special chars
HMACMachine = HMACSHA256(seed, "2")
pwd = re.sub("[^A-Za-z0-9]", "", AESCipher.bytesToBase64(HMACMachine.getBinDigest())) 

RTMP_ADDR = 'rtmp://127.0.0.1:1935/live/mystream' + '?psk=' + pwd
print("RTMP_ADDR: " + RTMP_ADDR)
# RTMP_ADDR = 'http://127.0.0.1:8000/live?port=1935&app=live&stream=mystream'
DUMMY_PROB = 1e-1
DUMMY_INTERVAL = 5.0


# Firebase authentication
cred = credentials.Certificate("privdoorbell-af796472f9a4.json")
firebase_admin.initialize_app(cred)

# Streaming source
if stream:
    vs = cv2.VideoCapture(RTMP_ADDR)
else:
    vs = VideoStream(src=0).start()

def send_to_token(token: str, msg_type='face'):
    try:
        with open("config.json") as f:
            settings = json.load(f)
            seed = settings['seed']
    except:
        print("Seed is not available. Try running init script again.", flush=True)
        return
    

    HMACMachine = HMACSHA256(seed, "1")
    AESMachine = AESCipher(HMACMachine.getBinDigest())
    '''
    ciphertext, tag = AESMachine.encrypt_base64(msg_type)
    timestamp = str(time.time())
    '''
    plaintext = "type: {}, timestamp: {}".format(msg_type, str(time.time()))
    ciphertext, tag = AESMachine.encrypt_base64(plaintext)
    # Restruct:
    # message = {
    # 'encrypted("type: face; timestamp: timestamp") + tag(24 char) + iv(16 char)' : '',
    # }
    '''
    message = messaging.Message(
        data={
            'type': ciphertext,
            'tag': tag,
            'iv': AESMachine.getIV_base64(),
            'timestamp': timestamp
        },
        token=token,
    )
    '''
    message = messaging.Message(
        data={
            ciphertext+tag+AESMachine.getIV_base64(): '',
        }
    )
    response = messaging.send(message)
    print("send_to_token(): " + str({
        'AESKey': AESCipher.bytesToBase64(HMACMachine.getBinDigest()),
        'type': ciphertext + ' (' + msg_type + ')',
        'tag': tag,
        'iv': AESMachine.getIV_base64(),
        'timestamp': timestamp + ' (' + StringHelper.timestamp2Readable(timestamp) + ')'
    }))
    print('send_to_token(): Attempted to send msg, res:', response, flush=True)



def send_dummy_packet():
    global notification_flag
    print("send_dummy_packet(): Started dummy packet thread", flush=True)
    starttime = time.time()
    #cryptogen = SystemRandom()
    while True:
        #if cryptogen.random() < DUMMY_PROB and not tokens.isEmpty():
        if not tokens.isEmpty():
            with notification_lock:
                if notification_flag == Param.NOTIFICATION_FACE:
                    for t in tokens.getList():
                        send_to_token(t, Param.NOTIFICATION_STRING_FACE)
                    notification_flag = Param.NOTIFICATION_STRING_NONE
                elif notification_flag == Param.NOTIFICATION_BELL:
                    for t in tokens.getList():
                        send_to_token(t, Param.NOTIFICATION_STRING_BELL)
                    notification_flag = Param.NOTIFICATION_STRING_NONE
                else:
                    for t in tokens.getList():
                        send_to_token(t, Param.NOTIFICATION_STRING_NONE)                    
        time.sleep(DUMMY_INTERVAL - ((time.time() - starttime) % DUMMY_INTERVAL))
        

@app.route("/")
def index():
    return render_template("index.html")

def bell_button_callback():
    global notification_flag
    with notification_lock:
        notification_flag = Param.NOTIFICATION_BELL

@app.route("/bell", methods = ['GET'])
def bell():
    global notification_flag
    with notification_lock:
        notification_flag = Param.NOTIFICATION_BELL
    return Param.HTTP_DEFAULT_RETURN

@app.route("/manageToken", methods = ['POST', 'GET'])
def manageToken():
    global tokens
    if request.method == 'GET':
        return render_template("token_management.html", tokens = tokens.getDict())
    elif request.method == 'POST':
        print("manageToken(): " + str(request.form.to_dict()), flush=True)
        for k, v in request.form.to_dict().items():
            print("manageToken(): " + k)
            tokens.delete(k)
        return render_template("token_management_confirmed.html")


@app.route("/register", methods = ['GET', 'POST'])
def register():
    global tokens
    if request.method == 'GET':
        return render_template("token_management.html", tokens = tokens.getDict())
    print("register(): Start recving post")
    data = request.form.to_dict()
    print("register(): " + str(data), flush=True)

    # Delimiter
    delim = "---"

    # If either is not available, return an error string
    try:
        with open("config.json") as f:
            settings = json.load(f)
            ret_msg = settings['seed'] + delim + settings['onion_hostname'] + delim + settings['onion_auth_cookie']
    except:
        return Param.HTTP_DEFAULT_RETURN
        
        
    firebase_token, nickname, device_token = StringHelper.extractFromPassedDict(data)
    tokens.insert(firebase_token, time.time(), device_token, nickname)
    print("register(): Returned " + ret_msg)
    tokens.dump()
    return ret_msg

@app.route("/video_feed")
def video_feed():
    return Response(generate(), mimetype = "multipart/x-mixed-replace; boundary=frame")

def detect_motion(frameCount):
    global vs, outputFrame, outputFrame_lock
    
    md = SingleMotionDetector(accumWeight=0.1)
    total = 0

    while True:
        if stream:
            _, frame = vs.read()
        else:
            frame = vs.read()

        frame = imutils.resize(frame, width=400)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        gray = cv2.GaussianBlur(gray, (7, 7), 0)

        timestamp = datetime.datetime.now()
        cv2.putText(frame, timestamp.strftime("%A %d %B %Y %I:%M:%S%p"), (10, frame.shape[0] - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (0, 0, 255), 1)

        if total > frameCount:
            motion = md.detect(gray)

            if motion is not None:
                print(motion, flush=True)
                (thresh, (minX, minY, maxX, maxY)) = motion
                cv2.rectangle(frame, (minX, minY), (maxX, maxY), (0, 0, 255), 2)
            else:
                #raise Exception('')
                pass

        md.update(gray)
        total += 1

        with outputFrame_lock:
            outputFrame = frame.copy()

def detect_face(frameCount):
    global vs, outputFrame, outputFrame_lock, notification_flag
    
    fd = OpenCVDetector()
    total = 0
    cur_time = time.time() - 15

    print_flag = True

    while True:
        num_faces = 0
        if stream:
            _, frame = vs.read()
        else:
            frame = vs.read()

        
        if not vs.isOpened() and print_flag:
            print("Empty capture object.")
            print_flag = False
            continue

        if frame is None and print_flag:
            print("Empty frame.")
            print_flag = False
            continue

        frame = imutils.resize(frame, width=400)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        gray = cv2.GaussianBlur(gray, (7, 7), 0)

        timestamp = datetime.datetime.now()
        cv2.putText(frame, timestamp.strftime("%A %d %B %Y %I:%M:%S%p"), (10, frame.shape[0] - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (0, 0, 255), 1)

        if total > frameCount:
            num_faces, faces = fd.detect(gray)

            if num_faces:
                #print(num_faces, faces, flush=True)
                for (x, y, w, h) in faces:
                    cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
            else:
                #raise Exception('')
                pass

        total += 1
        
        if num_faces:
            if (time.time() - cur_time > 30) and not tokens.isEmpty():
                with notification_lock:
                    notification_flag = Param.NOTIFICATION_FACE
                cur_time = time.time()
            else:
                print("detect_face(): " + str(time.time() - cur_time))
            
        with outputFrame_lock:
            outputFrame = frame.copy()


def generate():
    global outputFrame, outputFrame_lock
    
    while True:
        with outputFrame_lock:
            if outputFrame is None:
                continue

            (flag, encodedImage) = cv2.imencode(".jpg", outputFrame)

            if not flag:
                continue

        yield(b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + bytearray(encodedImage) + b'\r\n')


if __name__ == "__main__":
    ap = argparse.ArgumentParser()

    ap.add_argument("-f", "--frame-count", type=int, default=32)
    ap.add_argument("-d", "--detector", type=str, default='face')
    args = vars(ap.parse_args())

    # Multi-thread tasking

    # The detector thread
    if args['detector'] == 'face':
        detector_thread = threading.Thread(target = detect_face, args = (args["frame_count"], ))
    else:
        detector_thread = threading.Thread(target = detect_motion, args = (args["frame_count"], ))
    detector_thread.daemon = True
    detector_thread.start()

    # The dummy packet thread
    dummy_thread = threading.Thread(target = send_dummy_packet)
    dummy_thread.daemon = True
    dummy_thread.start()

    # GPIO
    doorbell_button.when_pressed = bell_button_callback


    app.run(host = "0.0.0.0", port = 8080, debug=True, threaded=True, use_reloader=False)

if not stream:
    vs.stop()
else:
    vs.release()