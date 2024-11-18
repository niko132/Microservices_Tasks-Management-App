import requests
import os

api_url_prefix = os.environ["API_URL_PREFIX"]
api_url_prefix_projects = api_url_prefix + "/projects"

def get_projects(token, user_id=None):
    headers = {"Authorization": "Bearer " + token}
    params = {"userId": user_id} if user_id else {}
    r = requests.get(api_url_prefix_projects, params=params, headers=headers, verify='./certs/cert.crt')
    return r.json()

def get_project(token, project_id):
    headers = {"Authorization": "Bearer " + token}
    r = requests.get(api_url_prefix_projects + '/' + str(project_id), headers=headers, verify='./certs/cert.crt')
    return r.json()
