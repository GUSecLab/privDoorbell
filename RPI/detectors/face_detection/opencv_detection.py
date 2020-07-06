import cv2

class OpenCVDetector(object):
    def __init__(self):
        self.clf = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")

    
    def detect(self, image):
        gray = image
        faces = self.clf.detectMultiScale(
            gray,
            scaleFactor=1.3,
            minNeighbors=3,
            minSize=(75, 75)
        )

        return (len(faces), faces)