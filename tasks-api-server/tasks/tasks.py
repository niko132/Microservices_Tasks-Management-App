import typing
from flask import Flask, request, jsonify
from flask_restful import Resource, Api, abort
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.inspection import inspect
from sqlalchemy.orm.collections import InstrumentedList
from dataclasses import dataclass
import os
from auth_middleware import authenticate
from projectclient import get_projects, get_project
from taskclient import get_task

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
class CommentModel(db.Model, Serializer):
    __tablename__ = 'comments'

    id: int = db.Column(db.Integer, primary_key=True)
    text: str = db.Column(db.Text, nullable=False)
    taskId: int = db.Column(db.Integer, db.ForeignKey('tasks.id'))
    userId: int = db.Column(db.Integer, nullable=False)

    task = db.relationship('TaskModel', backref=db.backref('comments', lazy=True))

    def __init__(self, text=None, taskId=None, userId=None, json={}):
        self.text = text if text else json.get('text', None)
        self.taskId = taskId if taskId else json.get('taskId', None)
        self.userId = userId if userId else json.get('userId', None)
    
    def serialize(self):
        d = super().serialize()
        del d['task']
        return d

@dataclass
class TaskModel(db.Model, Serializer):
    __tablename__ = 'tasks'

    id: int = db.Column(db.Integer, primary_key=True)
    projectId: int = db.Column(db.Integer, nullable=False)
    title: str = db.Column(db.String(256), nullable=False)
    description: str = db.Column(db.String(256), nullable=False)
    status: str = db.Column(db.String(256), nullable=False)
    assigneeIds: typing.List[int] = db.Column(db.ARRAY(db.Integer), nullable=False)

    def __init__(self, projectId=None, title=None, description=None, status=None, assigneeIds=[], json={}):
        self.projectId = projectId if projectId else json.get('projectId', None)
        self.title = title if title else json.get('title', None)
        self.description = description if description else json.get('description', None)
        self.status = status if status else json.get('status', None)
        self.assigneeIds = assigneeIds if assigneeIds else json.get('assigneeIds', [])
    
    def update(self, json={}):
        self.projectId = json.get('projectId', self.projectId)
        self.title = json.get('title', self.title)
        self.description = json.get('description', self.description)
        self.status = json.get('status', self.status)
        self.assigneeIds = json.get('assigneeIds', self.assigneeIds)

class TasksResource(Resource):
    @authenticate
    def get(current_user, self):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        projectId = request.args.get('projectId')

        tasks = TaskModel.query.filter(TaskModel.projectId == projectId, TaskModel.projectId.in_(all_project_ids)).all() if projectId else TaskModel.query.filter(TaskModel.projectId.in_(all_project_ids)).all()

        return jsonify([task.serialize() for task in tasks])

    @authenticate
    def post(current_user, self):
        json = request.get_json(force=True)
        task = TaskModel(json=json)

        project = get_project(current_user['token'], task.projectId)
        if not project:
            return abort(401)

        # TODO: validate input

        db.session.add(task)
        db.session.commit()

        return jsonify(task.serialize())

class TaskResource(Resource):
    @authenticate
    def get(current_user, self, taskId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        task = TaskModel.query.filter(TaskModel.id == taskId, TaskModel.projectId.in_(all_project_ids)).first()
        if not task:
            return error('Task does not exist')
        
        return jsonify(task.serialize())

    @authenticate
    def put(current_user, self, taskId):
        json = request.get_json(force=True)

        task = TaskModel.query.filter(TaskModel.id == taskId).first()
        if not task:
            return error('Task does not exist')
        
        project = get_project(current_user['token'], task.projectId)
        if not project:
            return abort(401)
        
        task.update(json)

        assignees = set(map(int, task.assigneeIds))
        task.assigneeIds = list(assignees)

        db.session.commit()

        return jsonify(task.serialize())

    @authenticate
    def delete(current_user, self, taskId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        task = TaskModel.query.filter(TaskModel.id == taskId, TaskModel.projectId.in_(all_project_ids)).first()
        if not task:
            return error('Task does not exist')
        
        db.session.delete(task)
        db.session.commit()

        return jsonify(task.serialize())

class TaskAssigneesResource(Resource):
    @authenticate
    def get(current_user, self, taskId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        task = TaskModel.query.filter(TaskModel.id == taskId, TaskModel.projectId.in_(all_project_ids)).first()
        if not task:
            return error('Task does not exist')
        
        return jsonify(task.assigneeIds)

    @authenticate
    def post(current_user, self, taskId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        json = request.get_json(force=True)
        task = TaskModel.query.filter(TaskModel.id == taskId, TaskModel.projectId.in_(all_project_ids)).first()
        if not task:
            return error('Task does not exist')

        task.assigneeIds = set(task.assigneeIds + list(json))

        db.session.add(task)
        db.session.commit()

        return jsonify(task.assigneeIds)

class TaskAssigneeResource(Resource):
    @authenticate
    def delete(current_user, self, taskId, userId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        task = TaskModel.query.filter(TaskModel.id == taskId, TaskModel.projectId.in_(all_project_ids)).first()
        if not task:
            return error('Task does not exist')
        
        task.assigneeIds = set(task.assigneeIds) - set([userId])

        db.session.add(task)
        db.session.commit()

        return jsonify(task.assigneeIds)

class TaskStatusResource(Resource):
    @authenticate
    def put(current_user, self, taskId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        json = request.get_json(force=True)
        task = TaskModel.query.filter(TaskModel.id == taskId, TaskModel.projectId.in_(all_project_ids)).first()
        if not task:
            return error('Task does not exist')
        
        status = json['status']
        if not status:
            return error('Status must not be None')
        
        task.status = status

        db.session.add(task)
        db.session.commit()

        return jsonify(task.status)

class TaskCommentsResource(Resource):
    @authenticate
    def get(current_user, self, taskId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        task = TaskModel.query.filter(TaskModel.id == taskId, TaskModel.projectId.in_(all_project_ids)).first()
        if not task:
            return error('Task does not exist')
        
        return jsonify(task.comments)
    
    @authenticate
    def post(current_user, self, taskId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        json = request.get_json(force=True)
        comment = CommentModel(taskId=taskId, json=json)

        task = get_task(current_user['token'], taskId)
        if not task or 'projectId' not in task or task['projectId'] not in all_project_ids:
            return abort(401)

        # TODO: validate input

        db.session.add(comment)
        db.session.commit()

        return jsonify(comment)

class TaskCommentResource(Resource):
    @authenticate
    def get(current_user, self, taskId, commentId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        comment = CommentModel.query.filter_by(id = commentId).first()
        if not comment:
            return error('Comment does not exist')
        
        if taskId is not comment.taskId:
            return error('Comment for Task does not exist')
        
        task = get_task(current_user['token'], taskId)
        if not task or 'projectId' not in task or task['projectId'] not in all_project_ids:
            return abort(401)
        
        return jsonify(comment)
    
    @authenticate
    def delete(current_user, self, taskId, commentId):
        current_user_id = current_user['id']

        all_projects = get_projects(current_user['token'], current_user_id)
        all_project_ids = [project['id'] for project in all_projects]

        comment = CommentModel.query.filter_by(id = commentId).first()
        if not comment:
            return error('Comment does not exist')
        
        if taskId is not comment.taskId:
            return error('Comment for Task does not exist')

        task = get_task(current_user['token'], taskId)
        if not task or 'projectId' not in task or task['projectId'] not in all_project_ids:
            return abort(401)
        
        db.session.delete(comment)
        db.session.commit()
        
        return jsonify(comment)

db.drop_all()
db.create_all() # TODO: remove
task1 = TaskModel(projectId=1, title='Task1', description='This is the first task', status='open', assigneeIds=[])
task2 = TaskModel(projectId=2, title='Task2', description='This is the second task', status='closed', assigneeIds=[])
db.session.add(task1)
db.session.add(task2)
db.session.commit()
comment1 = CommentModel(text='This is the first comment', taskId=task1.id, userId=1)
comment2 = CommentModel(text='This is the second comment', taskId=task2.id, userId=2)
db.session.add(comment1)
db.session.add(comment2)
db.session.commit()

api.add_resource(TasksResource, '/tasks')
api.add_resource(TaskResource, '/tasks/<int:taskId>')
api.add_resource(TaskAssigneesResource, '/tasks/<int:taskId>/assignees')
api.add_resource(TaskAssigneeResource, '/tasks/<int:taskId>/assignees/<int:userId>')
api.add_resource(TaskStatusResource, '/tasks/<int:taskId>/status')
api.add_resource(TaskCommentsResource, '/tasks/<int:taskId>/comments')
api.add_resource(TaskCommentResource, '/tasks/<int:taskId>/comments/<int:commentId>')

if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0")