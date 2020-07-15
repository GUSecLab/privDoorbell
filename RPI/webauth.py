import json
import re
import os

from flask import Response, Flask, render_template, request, abort

from cryptoutils import HMACSHA256, AESCipher

app_auth = Flask(__name__)


try:
    with open("config.json") as f:
        settings = json.load(f)
        seed = settings['seed']
except:
    print("Seed is not available. Try running init script again.", flush=True)
    exit(0)


HMACMachine = HMACSHA256(seed, "2")
pwd = re.sub("[^A-Za-z0-9]", "", AESCipher.bytesToBase64(HMACMachine.getBinDigest())) 

print("Password: " + pwd, flush=True)

@app_auth.route("/auth", methods = ['POST'])
def auth():
    '''
    Authentication function for RTMP on_play callback.
    It checks two fields: <psk> and <wmt>, where <psk> should contain the password
    and <wmt> should contain the username (device_token).

    <psk> = HMAC(seed, <wmt>).replaceAll("[^A-Za-z0-9]", "")
    '''
    d = request.form.to_dict()
    if 'psk' in d:
        if d['psk'] == pwd:
            return Response("", status=201)
        else:
            if 'wmt' in d:
                try:
                    with open("registration.json", "r") as f:
                        tokenList = json.load(f)
                    for _, (device_token, _, _) in tokenList:
                        if d['wmt'] == device_token and d['psk'] == re.sub("[^A-Za-z0-9]", "", AESCipher.bytesToBase64(HMACSHA256(seed, device_token).getBinDigest())):
                            return Response("", status=201)
                    return Response("", status=404)
                except:
                    return Response("", status=404)
            else:
                return Response("", status=404)
    else:
        return Response("", status=404)

@app_auth.route("/playAudio", methods = ['GET'])
def play_audio():
    d = request.form.to_dict()
    try:
        with open("registration.json", "r") as f:
            tokenList = json.load(f)
        for _, (device_token, _, _) in tokenList:
            if d['wmt'] == device_token and d['psk'] == re.sub("[^A-Za-z0-9]", "", AESCipher.bytesToBase64(HMACSHA256(seed, device_token).getBinDigest())):
                if d['audio'] == '1':
                    os.system('aplay 1.wav')
                elif d['audio'] == '2':
                    os.system('aplay 2.wav')
                elif d['audio'] == '3':
                    os.system('aplay 3.wav')
                else:
                    os.system('aplay 4.wav')
                return Response("", status=201)            
        return Response("", status=404)
    Response("", status=404)


if __name__ == "__main__":
    app_auth.run(host = "0.0.0.0", port = 8081, debug=True, threaded=True, use_reloader=False)