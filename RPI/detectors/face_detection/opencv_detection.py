import cv2

class OpenCVDetector(object):

    DETECTOR_FILES = {"EYE_TREE_EYEGLASSES": "haarcascade_eye_tree_eyeglasses.xml",
        "EYE": "haarcascade_eye.xml",
        "FRONTALCATFACE_EXTENDED": "haarcascade_frontalcatface_extended.xml",
        "FRONTALCATFACE": "haarcascade_frontalcatface.xml",
        "FRONTALCATFACE_ALT2": "haarcascade_frontalface_alt2.xml",
        "FRONTALCATFACE_ALT_TREE": "haarcascade_frontalface_alt_tree.xml",
        "FRONTALCATFACE_ALT": "haarcascade_frontalface_alt.xml",
        "FRONTALCATFACE_DEFAULT": "haarcascade_frontalface_default.xml",
        "FULLBODY": "haarcascade_fullbody.xml",
        "LEFTEYE_2SPLITS": "haarcascade_lefteye_2splits.xml",
        "LICENCE_PLATE_RUS_16STAGES": "haarcascade_licence_plate_rus_16stages.xml",
        "LOWERBODY": "haarcascade_lowerbody.xml",
        "PROFILEFACE": "haarcascade_profileface.xml",
        "RIGHTEYE_2SPLITS": "haarcascade_righteye_2splits.xml",
        "RUSSIAN_PLATE_NUMBER": "haarcascade_russian_plate_number.xml",
        "SMILE": "haarcascade_smile.xml",
        "UPPERBODY": "haarcascade_upperbody.xml",}



    def __init__(self, detector=None):
        self.clf = cv2.CascadeClassifier(cv2.data.haarcascades + self.DETECTOR_FILES.get(detector, "haarcascade_frontalface_default.xml"))

    
    def detect(self, image, scaleFactor=1.3, minNeighbors=3, minSize=(55, 55), maxSize=None):
        faces = self.clf.detectMultiScale(
            image,
            scaleFactor=scaleFactor,
            minNeighbors=minNeighbors,
            minSize=minSize
        )

        return (len(faces), faces)