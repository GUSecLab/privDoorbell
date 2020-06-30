from detectors.motion_detection.singlemotiondetector import SingleMotionDetector
from detectors.face_detection.opencv_detection import OpenCVDetector
from cryptoutils import HMACSHA256, AESCipher
from tokenList import TokenList
from utils.helper import StringHelper

import threading
import argparse
import datetime
import time
from random import SystemRandom

import imutils
from imutils.video import VideoStream
import cv2
from flask import Response, Flask, render_template, request
import firebase_admin
from firebase_admin import credentials, messaging

outputFrame = None
stream = True
tokens = TokenList()
outputFrame_lock = threading.Lock()
notification_lock = threading.Lock()

app = Flask(__name__)

#vs = VideoStream(src=0).start()
#time.sleep(2.0)
RTMP_ADDR = 'rtmp://127.0.0.1:1935/live/mystream'
DUMMY_PROB = 1e-1

if stream:
    vs = cv2.VideoCapture(RTMP_ADDR)
else:
    vs = VideoStream(src=0).start()

def send_to_token(token: str, msg_type='face'):
    # Know what you are doing here! The read() method comes with a newline at the end
    # Strip that or the server will be using a different key with the client
    with open("seed.conf") as f:
        seed = f.read().strip()
    print("Seed: " + seed)
    HMACMachine = HMACSHA256(seed, "1")
    AESMachine = AESCipher(HMACMachine.getBinDigest())

    ciphertext, tag = AESMachine.encrypt_base64(msg_type)

    message = messaging.Message(
        data={
            'type': ciphertext,
            'tag': tag,
            'iv': AESMachine.getIV_base64(),
            'timestamp': str(time.time())
        },
        token=token,
    )
    response = messaging.send(message)
    print({
        'AESKey': AESCipher.bytesToBase64(HMACMachine.getBinDigest()),
        'type': ciphertext,
        'tag': tag,
        'iv': AESMachine.getIV_base64(),
        'timestamp': str(time.time())     
    })
    print('Attempted to send msg, res:', response, flush=True)

cred = credentials.Certificate("privdoorbell-af796472f9a4.json")
firebase_admin.initialize_app(cred)

def send_dummy_packet():
    print("Started dummy packet thread", flush=True)
    starttime = time.time()
    cryptogen = SystemRandom()
    while True:
        if cryptogen.random() < DUMMY_PROB and not tokens.isEmpty():
            for t in tokens.getList():
                send_to_token(t, "dummy")
        time.sleep(60.0 - ((time.time() - starttime) % 60.0))
        

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/bell", methods = ['GET'])
def bell():
    global tokens
    for t in tokens.getList():
        send_to_token(t, 'bell')

@app.route("/manageToken", methods = ['POST', 'GET'])
def manageToken():
    global tokens
    if request.method == 'GET':
        return render_template("token_management.html", tokens = tokens.getDict())
    elif request.method == 'POST':
        print(request.form.to_dict(), flush=True)
        for k, v in request.form.to_dict().items():
            print(k)
            tokens.delete(k)
        return render_template("token_management_confirmed.html")


@app.route("/register", methods = ['POST'])
def register():
    global tokens
    print("Start recving post")
    data = request.form.to_dict()
    print(data, flush=True)

    # Delimiter
    ret_msg = "---"

    with open("seed.conf") as f:
        s = f.read()
    if not s:
        return 0
    else:
        ret_msg = s + ret_msg
    with open("hostname.conf") as f:
        s = f.read()
    if not s:
        return 0
    else:
        ret_msg = ret_msg + s
        
    token, nickname = StringHelper.extractFromPassedDict(data)
    tokens.insert(token, time.time(), nickname)
    print("Returned" + ret_msg)
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
    global vs, outputFrame, outputFrame_lock
    
    fd = OpenCVDetector()
    total = 0
    cur_time = time.time() - 15

    while True:
        num_faces = 0
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
            num_faces, faces = fd.detect(gray)

            if num_faces:
                print(num_faces, faces, flush=True)
                for (x, y, w, h) in faces:
                    cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
            else:
                #raise Exception('')
                pass

        total += 1

        if num_faces:
            if (time.time() - cur_time > 30) and not tokens.isEmpty():
                for t in tokens.getList():
                    send_to_token(t)
                print("Message sent.", flush=True)
                cur_time = time.time()
            else:
                print(time.time() - cur_time)
            
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
    ap.add_argument("-i", "--ip", type=str, default='0.0.0.0', help="ip address of device")
    ap.add_argument("-o", '--port', type=int, default=8080)
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


    app.run(host = args["ip"], port = args["port"], debug=True, threaded=True, use_reloader=False)

if not stream:
    vs.stop()