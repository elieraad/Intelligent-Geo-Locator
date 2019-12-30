# import the necessary packages
from keras.preprocessing.image import ImageDataGenerator # used to apply random transformations (rotations, shearing, ...)
from keras.optimizers import Adam                        # optimizer method used to train our network
from keras.preprocessing.image import img_to_array
from sklearn.preprocessing import LabelBinarizer         # transform label string to an integer
from sklearn.model_selection import train_test_split     # create our training and testing splits
from learner.geolocatorlearner import SmallerVGGNet      # import the implementation of our learner
from imutils import paths
import numpy as np
import random
import pickle
import cv2
import os

EPOCHS = 10                # number of epochs
INIT_LR = 1e-3              # initial learning rate 1e-3 is the default value for Adam optimizer
BS = 10                     # batch size
IMAGE_DIMS = (224, 224, 3)    # image dimensions 96 x 96  pixels with 3 channels (RGB)

# initialize the data and labels
data = []
labels = []

# grab the image paths and randomly shuffle them
print("[INFO] loading images...")
imagePaths = sorted(list(paths.list_images("dataset")))
random.seed(42)
random.shuffle(imagePaths)

count = 0
# loop over the input images
for imagePath in imagePaths:
	image = cv2.imread(imagePath)                               # load the image
	image = cv2.resize(image, (IMAGE_DIMS[1], IMAGE_DIMS[0]))   # resize it
	count = count + 1 
	print("[INFO] resizing image", count)
	image = img_to_array(image)                                 # convert the image to a Keras-compatible array 
	data.append(image)                                          # store it in the data list
 
	# extract the class label from the image path and update the
	# labels list
	label = imagePath.split(os.path.sep)[-2]
	labels.append(label)
	print("[INFO] image[" + str(count) + "] done")

data = np.array(data, dtype="float") / 255.0 # convert the data array to a NumPy array and scale it to the range  [0, 1]
labels = np.array(labels)                    # convert the labels  from a list to a NumPy array 

# binarize the labels
lb = LabelBinarizer()
labels = lb.fit_transform(labels)

# partition the data 80% for training and 20% for testing
(trainX, testX, trainY, testY) = train_test_split(data, labels, test_size=0.2, random_state=42)

# construct the image generator for data augmentation
aug = ImageDataGenerator(rotation_range=25, width_shift_range=0.1, height_shift_range=0.1, shear_range=0.2, zoom_range=0.2, horizontal_flip=True, fill_mode="nearest")

# initialize the model
print("[INFO] compiling model...")
model = SmallerVGGNet.build(width=IMAGE_DIMS[1], height=IMAGE_DIMS[0],
	depth=IMAGE_DIMS[2], classes=len(lb.classes_)) # initialize Keras CNN model
opt = Adam(lr=INIT_LR, decay=INIT_LR / EPOCHS)     # use Adam optimizer with learning rate decay
model.compile(loss="categorical_crossentropy", optimizer=opt,
	metrics=["accuracy"]) # compile model with categorical cross-entropy (> 2 classes)

# train the network
print("[INFO] training network...")
H = model.fit_generator(
	aug.flow(trainX, trainY, batch_size=BS),
	validation_data=(testX, testY),
	steps_per_epoch=len(trainX) // BS,
	epochs=EPOCHS, verbose=1)

# save the model to disk
print("[INFO] serializing network...")
model.save("locator.model")

# save the label binarizer to disk
print("[INFO] serializing label binarizer...")
f = open("lb.pickle", "wb")
f.write(pickle.dumps(lb))
f.close()