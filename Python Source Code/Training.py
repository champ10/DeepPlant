from data_prep import DataSelection
from keras.callbacks import ModelCheckpoint, CSVLogger
from keras.layers import Dense, Activation, Dropout, Convolution2D, MaxPooling2D, Flatten
from keras.models import Sequential

# Import required libraries

# Data Preparation
num_of_training_samples = 1500
num_of_testing_samples = 400

ds = DataSelection("potato/", 227, 227)
ds.get_dictionary()
X_train, Y_train = ds.get_data(num_of_training_samples)
X_train = X_train.reshape(num_of_training_samples, 3, 227, 227)
X_train /= 255.0
# import cv2;a = X_train[1, :, :, :];a = a.reshape(227, 227, 3);cv2.imshow("image", a/255.0);cv2.waitKey(0)

# Define Model
model = Sequential()
model.add(Convolution2D(16, 5, 5, border_mode='valid', input_shape=(3, 227, 227),
                        dim_ordering='th', init='glorot_normal'))
model.add(Activation('relu'))
model.add(MaxPooling2D(pool_size=(2, 2), border_mode='valid', dim_ordering='th'))
model.add(Convolution2D(32, 3, 3, border_mode='valid', dim_ordering='th', init='glorot_normal'))
model.add(Activation('relu'))
model.add(MaxPooling2D(pool_size=(2, 2), border_mode='valid', dim_ordering='th'))
# print model.output_shape
model.add(Flatten())
# print model.output_shape
model.add(Dense(128, init='glorot_normal'))
model.add(Activation('relu'))
model.add(Dropout(0.5))
model.add(Dense(3))
model.add(Activation('softmax'))
model.summary()

# To save the error after every iteration
csv_logger = CSVLogger('potato/Loss_acc_logs.csv')

# saves the model weights after each epoch if the validation loss decreased
checkpointer = ModelCheckpoint(filepath="potato/weights.epoch{epoch:02d}-val_loss{val_loss:.2f}.hdf5",
                               verbose=1)

# Set model parameters
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

# Train the model
history_ret = model.fit(X_train, Y_train, nb_epoch=20, batch_size=50, shuffle=True,
                        verbose=2, validation_split=0.1, callbacks=[checkpointer, csv_logger])

model.save('potato/final.hdf5')

# Fetch testing data and test the model
X_test, Y_test = ds.get_data(num_of_testing_samples)
X_test = X_test.reshape(num_of_testing_samples, 3, 227, 227)
X_test /= 255.0
test_result = model.evaluate(X_test, Y_test, verbose=1)
print("Test Loss: " + str(test_result[0]))
print("Test accuracy: " + str(test_result[1]))
