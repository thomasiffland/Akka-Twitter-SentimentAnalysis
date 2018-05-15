import os
import json
import numpy as np
import keras.preprocessing.text as kpt
from keras.preprocessing.text import Tokenizer
from keras.models import model_from_json
from flask import Flask
from flask import request
from flask.json import jsonify

app = Flask(__name__)
dir_path = os.path.dirname(os.path.realpath(__file__))

# we're still going to use a Tokenizer here, but we don't need to fit it
tokenizer = Tokenizer(num_words=3000)
# for human-friendly printing
labels = ['negative', 'positive']

# read in our saved dictionary
with open(dir_path + '/dictionary.json', 'r') as dictionary_file:
    dictionary = json.load(dictionary_file)

# this utility makes sure that all the words in your input
# are registered in the dictionary
# before trying to turn them into a matrix.
def convert_text_to_index_array(text):
    words = kpt.text_to_word_sequence(text)
    wordIndices = []
    for word in words:
        if word in dictionary:
            wordIndices.append(dictionary[word])
        else:
            print("'%s' not in training corpus; ignoring." %(word))
    return wordIndices

# read in your saved model structure
json_file = open(dir_path + '/model.json', 'r')
loaded_model_json = json_file.read()
json_file.close()
# and create a model from that
model = model_from_json(loaded_model_json)
# and weight your nodes with your saved values
model.load_weights(dir_path + '/model.h5')

@app.route('/')
def execModel():
    evalSentence = request.args.get('q', '')
    # format your input for the neural net
    testArr = convert_text_to_index_array(evalSentence)
    nn_input = tokenizer.sequences_to_matrix([testArr], mode='binary')
    # predict which bucket your input belongs in
    pred = model.predict(nn_input)

    return jsonify([labels[np.argmax(pred)], pred[0][np.argmax(pred)] * 100]);