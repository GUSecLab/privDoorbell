from detectors.motion_detection.singlemotiondetector import SingleMotionDetector
from detectors.face_detection.opencv_detection import OpenCVDetector

import threading
import argparse
import datetime
import time

import imutils
from imutils.video import VideoStream
import cv2
from flask import Response, Flask, render_template, request
import firebase_admin
from firebase_admin import credentials, messaging

outputFrame = None
stream = True
token = None
outputFrame_lock = threading.Lock()
notification_lock = threading.Lock()

app = Flask(__name__)

#vs = VideoStream(src=0).start()
#time.sleep(2.0)
rtmp_addr = 'rtmp://priviot.cs-georgetown.net:1935/live/mystream'

if stream:
    vs = cv2.VideoCapture(rtmp_addr)
else:
    vs = VideoStream(src=0).start()

def send_to_token(token: str):
    message = messaging.Message(
        data={
            'score': '850',
        },
        token=token,
    )
    response = messaging.send(message)
    print('Attempted to send msg, res:', response, flush=True)

cred = credentials.Certificate("privdoorbell-af796472f9a4.json")
firebase_admin.initialize_app(cred)

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/register", methods = ['POST', 'GET'])
def register():
    global token
    if request.method == 'POST':
        print("Start recving post")
        data = request.form.to_dict()
        print(data, flush=True)
        for k, v in data.items():
            token = k
        return 'Comfirmed'
    elif request.method == 'GET':
        if token:
            return token
        else:
            return "None"

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
    cur_time = time.time()

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
            if (time.time() - cur_time > 30) and token:
                send_to_token(token)
                cur_time = time.time()
            else:
                print(time.time() - cur_time, token)
            
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

    if args['detector'] == 'face':
        t = threading.Thread(target = detect_face, args = (args["frame_count"], ))
    else:
        t = threading.Thread(target = detect_motion, args = (args["frame_count"], ))
    t.daemon = True
    t.start()

    app.run(host = args["ip"], port = args["port"], debug=True, threaded=True, use_reloader=False)

if stream:
    vs.stop()