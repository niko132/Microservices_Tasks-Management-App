import requests
import os

api_url_prefix = os.environ["API_URL_PREFIX"]
api_url_prefix_users = api_url_prefix + "/users"

def get_users(token):
    headers = {"Authorization": "Bearer " + token}
    r = requests.get(api_url_prefix_users, headers=headers, verify='./certs/cert.crt')
    return r.json()

def get_user(token, user_id):
    headers = {"Authorization": "Bearer " + token}
    r = requests.get(api_url_prefix_users + '/' + str(user_id), headers=headers, verify='./certs/cert.crt')
    return r.json()

def login(email, password):
    r = requests.post(api_url_prefix_users + '/login', data={"email": email, "password": password}, verify='./certs/cert.crt')
    return r.json()