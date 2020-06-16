from Cryptodome.Cipher import AES
from Cryptodome.Hash import HMAC, SHA256
from base64 import b64encode
import random
import secrets

key = b'281742777473543518207051811201009247628'
msg = b'Crimson Humble God'

class HMACSHA256(object):

    def __init__(self, key, data):
        self.hmac = HMAC.new(key, digestmod=SHA256)
        self.hmac.update(data)
    
    def getBinDigest(self):
        return self.hmac.digest()
    
    def getHexDigest(self):
        return self.hmac.hexdigest()


class AESCipher(object):

    GCM_IV_LENGTH = 12
    GCM_TAG_LENGTH = 16

    def __init__(self, key):
        self.key = key
        self.iv = secrets.token_bytes(AESCipher.GCM_IV_LENGTH)

    def encrypt(self, data):
        cipher = AES.new(self.key, mode=AES.MODE_GCM, nonce=self.iv)
        #cipher.update(data)
        ciphertext, tag = cipher.encrypt_and_digest(data)
        return ciphertext, tag
    
    def encrypt_base64(self, data):
        ciphertext, tag = self.encrypt(data)
        return self.bytesToBase64(ciphertext), self.bytesToBase64(tag)

    def getIV(self):
        return self.iv

    def getIV_base64(self):
        return self.bytesToBase64(iv)

    @staticmethod
    def bytesToBase64(byteString):
        return b64encode(byteString).decode('utf-8')
    


if __name__ == "__main__":
    HMACMachine = HMACSHA256(key, msg)
    new_key = HMACMachine.getBinDigest()
    aes = AESCipher(new_key)


    print(AESCipher.bytesToBase64(new_key))
    print(aes.encrypt_base64(msg), AESCipher.bytesToBase64(aes.getIV()))

    