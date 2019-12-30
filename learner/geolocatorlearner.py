# import the necessary packages
from keras.models import Sequential
from keras.layers.normalization import BatchNormalization
from keras.layers.convolutional import Conv2D
from keras.layers.convolutional import MaxPooling2D
from keras.layers.core import Activation
from keras.layers.core import Flatten
from keras.layers.core import Dropout
from keras.layers.core import Dense
from keras import backend as K

class SmallerVGGNet:
    # width : The image width dimension.
    # height : The image height dimension.
    # depth : The depth of the image — also known as the number of channels.
    # classes : The number of classes in our dataset (LAU buildings)
	@staticmethod
	def build(width, height, depth, classes):
		model = Sequential()		        # initialize the model
		inputShape = (height, width, depth) # input shape is "channels last"
		chanDim = -1                        # channels dimension (last)

		if K.image_data_format() == "channels_first":   # if we are using "channels first"
			inputShape = (depth, height, width)         # update the input shape
			chanDim = 1                                 # update channels dimension (first)

        # CONV => RELU => POOL
		model.add(Conv2D(32, (3, 3), padding="same", input_shape=inputShape))   # convolution layer has 32  filters with a 3 x 3  kernel
		model.add(Activation("relu"))                                           # activation function: rectified linear unit
		model.add(BatchNormalization(axis=chanDim))                             # normalize the channel values
		model.add(MaxPooling2D(pool_size=(3, 3)))                               # reduce spatial dimensions by 3
		model.add(Dropout(0.25))                                                # randomly disconnecting nodes to introduce redundancy 

        # (CONV => RELU) * 2 => POOL
        # Stacking multiple layers together (prior to reducing the spatial dimensions of the volume) 
        # allows for a richer set of features.
		model.add(Conv2D(64, (3, 3), padding="same"))   # increase filter size from 32 to 64
		model.add(Activation("relu"))
		model.add(BatchNormalization(axis=chanDim))
		model.add(Conv2D(64, (3, 3), padding="same"))
		model.add(Activation("relu"))
		model.add(BatchNormalization(axis=chanDim))
		model.add(MaxPooling2D(pool_size=(2, 2)))       # decrease max pooling size from 3 x 3  to 2 x 2
		model.add(Dropout(0.25))

        # (CONV => RELU) * 2 => POOL
		model.add(Conv2D(128, (3, 3), padding="same"))  # increase filter size to 128
		model.add(Activation("relu"))
		model.add(BatchNormalization(axis=chanDim))
		model.add(Conv2D(128, (3, 3), padding="same"))
		model.add(Activation("relu"))
		model.add(BatchNormalization(axis=chanDim))
		model.add(MaxPooling2D(pool_size=(2, 2)))
		model.add(Dropout(0.25))

        # first (and only) set of FC => RELU layers
		model.add(Flatten())
		model.add(Dense(1024))                          # fully connected layer with 1024 outputs
		model.add(Activation("relu"))
		model.add(BatchNormalization())
		model.add(Dropout(0.5))                         # dropout of 50% for fully-connected layers

		# softmax classifier - return the predicted probabilities for each class label.
		model.add(Dense(classes))
		model.add(Activation("softmax"))

		# return the constructed network architecture
		return model