from imutils.video import VideoStream
from flask import Response, Flask, render_template

from md.motion_detection.singlemotiondetector import SingleMotionDetector

import threading
import argparse
import datetime
import time

import imutils
import cv2

outputFrame = None
lock = threading.Lock()

app = Flask(__name__)

#vs = VideoStream(src=0).start()
#time.sleep(2.0)
rtmp_addr = 'rtmp://priviot.cs-georgetown.net:1935/live/mystream'
cap = cv2.VideoCapture(rtmp_addr)
#vs = VideoStream(src=0).start()


@app.route("/")
def index():
    return render_template("index.html")

@app.route("/video_feed")
def video_feed():
    return Response(generate(), mimetype = "multipart/x-mixed-replace; boundary=frame")

def detect_motion(frameCount):
    #global vs, outputFrame, lock
    global cap, outputFrame, lock
    
    md = SingleMotionDetector(accumWeight=0.1)
    total = 0

    while True:
        #frame = vs.read()
        _, frame = cap.read()

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

        with lock:
            outputFrame = frame.copy()

def generate():
    global outputFrame, lock
    
    while True:
        with lock:
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
    args = vars(ap.parse_args())

    t = threading.Thread(target = detect_motion, args = (args["frame_count"], ))
    t.daemon = True
    t.start()

    app.run(host = args["ip"], port = args["port"], debug=True, threaded=True, use_reloader=False)

vs.stop()