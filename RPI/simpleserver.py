import socket
import os
from threading import Thread
import threading
import time
import select
import sys

from flask import Response, Flask, render_template


app = Flask(__name__)
outputFrame = None

class Server(object):
    def __init__(self, host='0.0.0.0', port=8000, buffer_size=8):
        self.__host = host
        self.__port = port
        self.__buffer_size = buffer_size

        self.socket = self.__initiate()
    
    def __initiate(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            s.bind((self.__host, self.__port))
            s.listen(1)
        except:
            raise Exception("Failed to bind port")
        return s
    
    def run(self):
        global outputFrame
        print("Running socket on port 8000", flush = True)

        addr_list = []

        self.rlist, self.wlist, self.xlist = [self.socket], [], []

        count = 0
        while True:
            count += 1
            to_read, to_write, to_exc = select.select(self.rlist, self.wlist, self.xlist)

            if self.socket in to_read:
                conn, addr = self.socket.accept()
                self.rlist.append(conn)
                addr_list.append(addr)
            
            for c in to_read:
                if not c is self.socket:
                    try:
                        header = c.recv(self.__buffer_size)
                    except:
                        raise Exception('')
                        
                    packagesize = int.from_bytes(header, 'big')
                    #print(packagesize, file=sys.stderr)

                    try:
                        data = c.recv(packagesize, socket.MSG_WAITALL)
                    except:
                        raise Exception('')
                
                    if data:
                        #print(data, file=sys.stderr)
                        outputFrame = data
                    else:
                        if c in self.rlist:
                            self.rlist.remove(c)
                        c.close()
def generate():
    global outputFrame
    while True:
        if outputFrame:
            #print("1", flush=True)
            yield outputFrame
        else:
            yield b''
    

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/video_feed")
def video_feed():
    return Response(generate(), mimetype = "multipart/x-mixed-replace; boundary=frame")


if __name__ == "__main__":
    s = Server()
    t = threading.Thread(target = s.run)
    t.daemon = True
    t.start()

    app.run(host = '0.0.0.0', port=9000, debug=True, threaded=True, use_reloader=False)