from Cryptodome.Cipher import AES
from Cryptodome.Hash import HMAC, SHA256
from base64 import b64encode
import random
import secrets

class HMACSHA256(object):

    def __init__(self, key, data):
        if isinstance(key, str):
            key = key.encode()
        if isinstance(data, str):
            data = data.encode()
        self.hmac = HMAC.new(key, digestmod=SHA256)
        self.hmac.update(data)
    
    def getBinDigest(self):
        return self.hmac.digest()
    
    def getHexDigest(self):
        return self.hmac.hexdigest()


class AESCipher(object):

    # The following static variables should match with the client
    GCM_IV_LENGTH = 12
    GCM_TAG_LENGTH = 16

    def __init__(self, key):
        if isinstance(key, str):
            key = key.encode()

        self.key = key
        self.iv = secrets.token_bytes(AESCipher.GCM_IV_LENGTH)

    def encrypt(self, data):
        if isinstance(data, str):
            data = data.encode()

        cipher = AES.new(self.key, mode=AES.MODE_GCM, nonce=self.iv)
        #cipher.update(data)
        ciphertext, tag = cipher.encrypt_and_digest(data)
        return ciphertext, tag
    
    def encrypt_base64(self, data):
        if isinstance(data, str):
            data = data.encode()
        ciphertext, tag = self.encrypt(data)
        return self.bytesToBase64(ciphertext), self.bytesToBase64(tag)

    def getIV(self):
        return self.iv

    def getIV_base64(self):
        return self.bytesToBase64(self.iv)

    @staticmethod
    def bytesToBase64(byteString):
        return b64encode(byteString).decode('utf-8')
