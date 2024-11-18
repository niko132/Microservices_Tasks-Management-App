from flask import Flask, request, jsonify
from flask_restful import Resource, Api, abort
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.inspection import inspect
from sqlalchemy.orm.collections import InstrumentedList
from dataclasses import dataclass
import os
from auth_middleware import generate_token, authenticate, authenticate_user_id
import bcrypt

app = Flask(__name__)
app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv("DATABASE_URL", "sqlite://")
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
db = SQLAlchemy(app)
api = Api(app)

def error(message):
    return {
        'message': message
    }, 400

class Serializer(object):

    def serialize(self):
        serializedObject= {}
        for c in inspect(self).attrs.keys():
            attribute = getattr(self, c)
            if(type(attribute) is InstrumentedList):
                serializedObject[c]= Serializer.serialize_list(attribute)
            else:
                serializedObject[c]= attribute                
        return serializedObject

    @staticmethod
    def serialize_list(l):
        return [m.serialize() for m in l]

@dataclass
class UserModel(db.Model, Serializer):
    __tablename__ = 'users'

    id: int = db.Column(db.Integer, primary_key=True)
    username: str = db.Column(db.String(256), nullable=False)
    email: str = db.Column(db.String(256), nullable=False)
    hashed_password: str = db.Column(db.String(256), nullable=False)

    def __init__(self, username=None, email=None, password=None, json={}):
        self.username = username if username else json.get('username', None)
        self.email = email if email else json.get('email', None)

        password = password if password else json.get('password', None)
        self.hashed_password = bcrypt.hashpw(str.encode(password), bcrypt.gensalt()).decode()
    
    def serialize(self, token=None):
        d = super().serialize()
        del d['hashed_password']
        if token:
            d['token'] = token
        return d

class UsersResource(Resource):
    @authenticate
    def get(current_user, self):
        userEmail = request.args.get('email')
        users = UserModel.query.filter(UserModel.email == userEmail).all() if userEmail else UserModel.query.all()
        return jsonify([user.serialize() for user in users])

    def post(self):
        json = request.get_json(force=True)
        user = UserModel(json=json)

        # TODO: validate input

        db.session.add(user)
        db.session.commit()

        token = generate_token(user.id)
        return jsonify(user.serialize(token))

class UserResource(Resource):
    @authenticate_user_id # to prevent infinite recursion
    def get(current_user_id, self, userId):

        # The client need to get information of other users so disable the following protection
        #if current_user_id is not userId:
        #    return abort(401)

        user = UserModel.query.filter_by(id = userId).first()
        if not user:
            return error('User does not exist')

        return jsonify(user.serialize())

    @authenticate
    def delete(current_user, self, userId):
        if current_user['id'] is not userId:
            return abort(401)

        user = UserModel.query.filter_by(id = userId).first()
        if not user:
            return error('User does not exist')
        
        db.session.delete(user)
        db.session.commit()

        return jsonify(user.serialize())

class LoginResource(Resource):
    def post(self):
        json = request.get_json(force=True)

        # TODO: validate

        email = json['email']
        password = json['password']

        user = UserModel.query.filter_by(email = email).first()
        if not user:
            return error('Wrong credentials')
        
        if not bcrypt.checkpw(str.encode(password), user.hashed_password.encode()):
            return error('Wrong credentials')
        
        token = generate_token(user.id)
        return jsonify(user.serialize(token))

db.drop_all()
db.create_all() # TODO: remove
db.session.add(UserModel(username='User1', email='user1@example.org', password='user1pass'))
db.session.add(UserModel(username='User2', email='user2@example.org', password='user2pass'))
db.session.commit()

api.add_resource(UsersResource, '/users')
api.add_resource(UserResource, '/users/<int:userId>')
api.add_resource(LoginResource, '/users/login')

if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0")