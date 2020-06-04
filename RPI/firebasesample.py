import time
import datetime

import firebase_admin
from firebase_admin import credentials, messaging


def send_to_token(token: str):
    message = messaging.Message(
        data={
            'score': '850',
        },
        token=token,
    )
    response = messaging.send(message)
    print('Successfully sent message:', response)



if __name__ == "__main__":
    cred = credentials.Certificate("privdoorbell-af796472f9a4.json")
    firebase_admin.initialize_app(cred)
    send_to_token('eK9wkNDuRc6SpJI3nN5JZI:APA91bF3htL6scGIt9L4AFJiNPsqeW2BPfraWI76b9s_Q29hS5qpNcchtsOlm2Vic52cxCgyczW3Bdtn1BIZemx8SV4jcM24C_sLNwKsyYdLFVHUi8vdEjo-FXr041t2gLm4Xe-SQ3vp')