import requests
import os

api_url_prefix = os.environ["API_URL_PREFIX"]
api_url_prefix_tasks = api_url_prefix + "/tasks"

def get_task(token, task_id):
    headers = {"Authorization": "Bearer " + token}
    r = requests.get(api_url_prefix_tasks + '/' + str(task_id), headers=headers, verify='./certs/cert.crt')
    return r.json()
