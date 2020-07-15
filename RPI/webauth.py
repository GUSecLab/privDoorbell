import json
import re

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
    d = request.form.to_dict()
    if 'psk' in d:
        if d['psk'] == pwd:
            return Response("", status=201)
        else:
            return Response("", status=404)
    else:
        return Response("", status=404)


if __name__ == "__main__":
    app_auth.run(host = "0.0.0.0", port = 8081, debug=True, threaded=True, use_reloader=False)