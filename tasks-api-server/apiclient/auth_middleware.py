from functools import wraps
import jwt
from flask import request
from userclient import get_user
import os

secret_key = os.environ["JWT_SECRET_KEY"]

def generate_token(user_id):
    token = jwt.encode({"user_id": user_id}, secret_key, algorithm="HS256")
    return token

def authenticate(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        if "Authorization" in request.headers:
            token = request.headers["Authorization"].split(" ")[1]
        if not token:
            return {
                "message": "Authentication Token is missing!",
                "data": None,
                "error": "Unauthorized"
            }, 401

        data = jwt.decode(token, secret_key, algorithms=["HS256"])
        current_user = get_user(token, data["user_id"])
        current_user['token'] = token

        if current_user is None:
            return {
                "message": "Invalid Authentication token!",
                "data": None,
                "error": "Unauthorized"
            }, 401

        return f(current_user, *args, **kwargs)

    return decorated

def authenticate_user_id(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        if "Authorization" in request.headers:
            token = request.headers["Authorization"].split(" ")[1]
        if not token:
            return {
                "message": "Authentication Token is missing!",
                "data": None,
                "error": "Unauthorized"
            }, 401
        try:
            data = jwt.decode(token, secret_key, algorithms=["HS256"])
            user_id = data["user_id"]
            
            if user_id is None:
                return {
                    "message": "Invalid Authentication token!",
                    "data": None,
                    "error": "Unauthorized"
                }, 401
        except Exception as e:
            return {
                "message": "Something went wrong",
                "data": None,
                "error": str(e)
            }, 500

        return f(user_id, *args, **kwargs)

    return decorated