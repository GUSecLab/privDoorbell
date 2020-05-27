from imutils.video import VideoStream
from flask import Response, Flask, render_template

import imutils
import cv2

from pyimagesearch.motion_detection.singlemotiondetector import SingleMotionDetector

import threading
import argparse
import datetime
import time
import select
import sys
import socket



outputFrame = None
lock = threading.Lock()

app = Flask(__name__)

rtmp_addr = 'rtmp://priviot.cs-georgetown.net:1935/live/mystream'
cap = cv2.VideoCapture(rtmp_addr)
#vs = VideoStream(src=0).start()

def get_frame(frameCount):
    #global vs, outputFrame, lock
    global cap, outputFrame, lock

    while True:
        #frame = vs.read()
        error, frame = cap.read()
        with lock:
            outputFrame = frame.copy()


def detect_motion(frameCount):
    global vs, outputFrame, lock
    
    md = SingleMotionDetector(accumWeight=0.1)
    total = 0

    while True:
        frame = vs.read()

        frame = imutils.resize(frame, width=400)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        gray = cv2.GaussianBlur(gray, (7, 7), 0)

        timestamp = datetime.datetime.now()
        cv2.putText(frame, timestamp.strftime("%A %d %B %Y %I:%M:%S%p"), (10, frame.shape[0] - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (0, 0, 255), 1)

        if total > frameCount:
            motion = md.detect(gray)

            if motion is not None:
                #print(motion, flush=True)
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

class Client(object):
    def __init__(self, server="149.28.45.168", port=9200, buffer_size=8):
        self.__port = port
        self.__server = server
        self.__buffer_size = buffer_size

        self.socket = self.__initiate()

    def __initiate(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        try:
            s.connect((self.__server, self.__port))
        except:
            raise Exception('Connection Error')

        return s
    
    def run(self):
        if not self.socket:
            raise Exception('')

        while True:
            g = generate()
            img = next(g)
            self.socket.send(len(img).to_bytes(8, 'big'))

            self.socket.send(img)
        
        self.shut()

            
    def shut(self):
        if self.socket:
            self.socket.close()




if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("-i", "--ip", type=str, required=True, help="ip address of device")
    ap.add_argument("-o", '--port', type=int, required=True)
    ap.add_argument("-f", "--frame-count", type=int, default=32)
    args = vars(ap.parse_args())

    t = threading.Thread(target = detect_motion, args = (args["frame_count"], ))
    t.daemon = True
    t.start()

    c = Client()
    c.run()


vs.stop()