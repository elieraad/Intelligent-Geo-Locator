# USAGE
# Start the server:
# 	python run_keras_server.py
# Submit a request via cURL:
# 	curl -X POST -F image=@dog.jpg 'http://localhost:5000/predict'
# Submita a request via Python:
#	python simple_request.py

# import the necessary packages
from keras.preprocessing.image import img_to_array
from keras.models import load_model
import tensorflow as tf
from PIL import Image
import numpy as np
import flask
import io
import pickle
import cv2
import base64

# initialize our Flask application and the Keras model
app = flask.Flask(__name__)
app.config['ENV'] = 'development'
app.config['DEBUG'] = True
app.config['TESTING'] = True

model = None
lb = None
graph = []

def run_model():
	# load the pre-trained Keras model (here we are using a model
	# pre-trained on ImageNet and provided by Keras, but you can
	# substitute in your own networks just as easily)
	global model
	global graph
	global lb

	model = load_model("locator.model")
	graph = tf.get_default_graph()
	lb = pickle.loads(open("lb.pickle", "rb").read())


def prepare_image(image, target):
	# if the image mode is not RGB, convert it
	if image.mode != "RGB":
		image = image.convert("RGB")

	# resize the input image and preprocess it
	image = image.resize(target)
	image = img_to_array(image) / 255.0
	image = np.expand_dims(image, axis=0)

	# return the processed image
	return image

@app.route("/predict", methods=["POST"])
def predict():
	# initialize the data dictionary that will be returned from the
	# view
	print("Start...")
	data = {"success": False}

	# ensure an image was properly uploaded to our endpoint
	if flask.request.method == "POST":
		if flask.request.form.get("image"):
			# read the image in PIL format
			print("Reading...")
			image = flask.request.form["image"]
			imgdata = base64.b64decode(str(image))
			image = Image.open(io.BytesIO(imgdata))
			print("Image read successfully")
			# preprocess the image and prepare it for classification
			image = prepare_image(image, target=(96, 96))
			print("Image processed successfully")

			# classify the input image and then initialize the list
			# of predictions to return to the client
			with graph.as_default():
				proba = model.predict(image)[0]
				idx = np.argmax(proba)
				label = lb.classes_[idx]
				
			print("Image classified successfully")
			data["predictions"] = []
			# loop over the results and add them to the list of
			# returned predictions
			r = {"label": label, "probability": proba[idx] * 100}
			print(r)
			data["predictions"].append(r)

			# indicate that the request was a success
			data["success"] = True

	# return the data dictionary as a JSON response
	return flask.jsonify(data)

# if this is the main thread of execution first load the model and
# then start the server
if __name__ == "__main__":
	print(("* Loading Keras model and Flask starting server..."
		"please wait until server has fully started"))
	run_model()
	app.run(host= '0.0.0.0')