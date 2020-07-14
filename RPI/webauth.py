from flask import Response, Flask, render_template, request, abort

app_auth = Flask(__name__)

@app_auth.route("/auth", methods = ['POST'])
def auth():
    d = request.form.to_dict()
    if 'psk' in d:
        if d['psk'] == 'absolutelysafepassword':
            return Response("", status=201)
        else:
            return Response("", status=404)
    else:
        return Response("", status=404)


if __name__ == "__main__":
    app_auth.run(host = "0.0.0.0", port = 8081, debug=True, threaded=True, use_reloader=False)