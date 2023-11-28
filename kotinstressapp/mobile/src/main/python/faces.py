import cv2
import dlib
import matplotlib.pyplot as plt
import numpy as np
def preprocess(file_path):
    detector = dlib.get_frontal_face_detector()
    img = cv2.imread(file_path)
    # Check if the image is not empty
    if img is None:
        print('Error: Unable to load the image.')
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = detector(gray)

    if len(faces) > 0:
        first_face = faces[0]

        x, y, w, h = first_face.left(), first_face.top(), first_face.width(), first_face.height()
        #for some reason, coordinates start at top left instead of bottom right like you would expect...
        face_roi = gray[y:y+h, x:x+w]

        resized_face = cv2.resize(face_roi, (48, 48))

        # Display the resized face(for troubleshooting)
        #plt.imshow(resized_face, cmap='gray')
        #plt.show()
        face_array = np.array(resized_face, dtype=np.float32) / 255.0
        input_data = face_array
        input_data.shape = (48*48)
        return input_data.tolist()
    else:
        print("error: face not detected")
        return [0.0] * (48*48)
