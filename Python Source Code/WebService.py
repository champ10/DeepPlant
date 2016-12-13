import json
import os
import time
import uuid

import bottle
import cv2
import numpy as np
from PIL import Image
from bottle import route, run, request
from bottle import static_file
from keras.models import load_model

bottle.BaseRequest.MEMFILE_MAX = 1024 * 1024 * 1024 * 1024  # (Max size of file)


@route('/isWorking')
def is_working():
    return "Web service Working fine"


@route('/index')
def server_static():
    return static_file("index.html", root='templates/')


@route('/UI', method='POST')
def find_disease():
    crop_name = request.forms.get('cropName')
    upload = request.files.get('upload')
    filename, ext = os.path.splitext(upload.filename)
    if ext not in ('.png', '.jpg', '.jpeg'):
        response = ({'Crop': crop_name, 'Disease': 'File extension not allowed.', 'Time': str(0)})
        return json.dumps(response)

    message = crop_name + ":" + filename
    print(message)
    save_file_name = "uploads/" + str(uuid.uuid4()) + "." + ext;
    upload.save(save_file_name)
    start_time = time.time()
    disease_name = process_image(crop_name, save_file_name)
    time_taken = (time.time() - start_time)
    response = ({'Crop': crop_name, 'Disease': disease_name, 'Time': str(time_taken)})
    result = json.dumps(response)
    print(result)
    return result


@route('/findDisease', method='POST')
def find_disease():
    crop_name = request.params.cropName
    upload = request.files.get('upload')
    filename, ext = os.path.splitext(upload.filename)
    if ext not in ('.png', '.jpg', '.jpeg'):
        response = ({'Crop': crop_name, 'Disease': 'File extension not allowed.', 'Time': str(0)})
        return json.dumps(response)

    message = crop_name + ":" + filename
    print(message)
    save_file_name = "uploads/" + str(uuid.uuid4()) + "." + ext;
    upload.save(save_file_name)
    start_time = time.time()
    disease_name = process_image(crop_name, save_file_name)
    time_taken = (time.time() - start_time)
    response = ({'Crop': crop_name, 'Disease': disease_name, 'Time': str(time_taken)})
    print(json.dumps(response))
    return json.dumps(response)


def process_image(crop_name, file_name):
    img = Image.open(file_name)
    # resize and normalize
    temp = np.asarray(img)
    X_test = temp / 255.0
    X_test = cv2.resize(X_test, (227, 227))
    X_test = X_test.reshape(1, 3, 227, 227)

    # load model and predict user input
    if (crop_name.lower() == 'corn'.lower()):
        model = load_model('models/corn.hdf5')
    elif (crop_name.lower() == 'apple'.lower()):
        model = load_model('models/apple.hdf5')
    elif (crop_name.lower() == 'potato'.lower()):
        model = load_model('models/potato.hdf5')
    else:
        model = load_model('models/corn.hdf5')

    test_result = model.predict_classes(X_test, verbose=1, batch_size=1)

    print(str(test_result[0]))
    ret = get_category(crop_name, test_result[0])
    print(ret)
    return ret


def get_category(crop_name, result):
    if (crop_name.lower() == 'corn'.lower()):
        if result == 0:
            ret = "Grey leaf spot"
        elif result == 1:
            ret = "Common rust"
        elif result == 2:
            ret = "Healthy"
        elif result == 3:
            ret = "Leaf blight"
    elif (crop_name.lower() == 'apple'.lower()):
        if result == 0:
            ret = "Apple scab"
        elif result == 1:
            ret = "Black rot"
        elif result == 2:
            ret = "Cedar Apple rust"
        elif result == 3:
            ret = "Healthy"
    elif (crop_name.lower() == 'potato'.lower()):
        if result == 0:
            ret = "Early blight"
        elif result == 1:
            ret = "Healthy"
        elif result == 2:
            ret = "Late blight"
    return ret


run(host='192.168.1.50', port=8091, debug=True)
