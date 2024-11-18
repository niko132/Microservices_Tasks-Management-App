import typing
from flask import Flask, request, jsonify
from flask_restful import Resource, Api
from flask_sqlalchemy import SQLAlchemy
from dataclasses import dataclass
import os
from auth_middleware import authenticate

app = Flask(__name__)
app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv("DATABASE_URL", "sqlite://")
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
db = SQLAlchemy(app)
api = Api(app)

def error(message):
    return {
        'message': message
    }, 400

@dataclass
class ProjectModel(db.Model):
    __tablename__ = 'projects'

    id: int = db.Column(db.Integer, primary_key=True)
    name: str = db.Column(db.String(256), nullable=False)
    ownerId: int = db.Column(db.Integer, nullable=False)
    memberIds: typing.List[int] = db.Column(db.ARRAY(db.Integer), nullable=False)

    def __init__(self, name=None, ownerId=None, memberIds=[], json={}):
        self.name = name if name else json.get('name', None)
        self.ownerId = ownerId if ownerId else json.get('ownerId', None)
        self.memberIds = memberIds if memberIds else json.get('memberIds', [])
    
    def update(self, json={}):
        self.name = json.get('name', self.name)
        self.ownerId = json.get('ownerId', self.ownerId)
        self.memberIds = json.get('memberIds', self.memberIds)

class ProjectsResource(Resource):
    @authenticate
    def get(current_user, self):
        current_user_id = current_user['id']

        userId = request.args.get('userId')
        projects = ProjectModel.query.filter(ProjectModel.memberIds.any(userId), ProjectModel.memberIds.any(current_user_id)).all() if userId else ProjectModel.query.filter(ProjectModel.memberIds.any(current_user_id)).all()
        return jsonify(projects)

    @authenticate
    def post(current_user, self):
        json = request.get_json(force=True)
        project = ProjectModel(json=json)

        members = set(project.memberIds) # ensure that members are unique
        if project.ownerId is not None: members.add(project.ownerId) # ensure that the owner is member if its project
        project.memberIds = list(members)

        db.session.add(project)
        db.session.commit()

        return jsonify(project)

class ProjectResource(Resource):
    @authenticate
    def get(current_user, self, projectId):
        current_user_id = current_user['id']

        project = ProjectModel.query.filter(ProjectModel.id == projectId, ProjectModel.memberIds.any(current_user_id)).first()
        if not project:
            return error('Project does not exist')
        
        return jsonify(project)
    
    @authenticate
    def put(current_user, self, projectId):
        current_user_id = current_user['id']
        json = request.get_json(force=True)

        project = ProjectModel.query.filter(ProjectModel.id == projectId, ProjectModel.memberIds.any(current_user_id)).first()
        if not project:
            return error('Project does not exist')

        project.update(json)

        members = set(project.memberIds) # ensure that members are unique
        if project.ownerId is not None: members.add(project.ownerId) # ensure that the owner is member if its project
        members = set(map(int, members))
        project.memberIds = list(members)

        db.session.commit()

        return jsonify(project)
    
    @authenticate
    def delete(current_user, self, projectId):
        current_user_id = current_user['id']

        project = ProjectModel.query.filter(ProjectModel.id == projectId, ProjectModel.ownerId == current_user_id).first()
        if not project:
            return error('Project does not exist')
        
        db.session.delete(project)
        db.session.commit()

        return jsonify(project)

class ProjectUserResource(Resource):
    @authenticate
    def get(current_user, self, projectId):
        current_user_id = current_user['id']

        project = ProjectModel.query.filter(ProjectModel.id == projectId, ProjectModel.memberIds.any(current_user_id)).first()
        if not project:
            return error('Project does not exist')
        
        return jsonify(project.memberIds)

db.drop_all()
db.create_all() # TODO: remove
db.session.add(ProjectModel(name='Test Project 1', ownerId=1, memberIds=[1]))
db.session.add(ProjectModel(name='Test Project 2', ownerId=2, memberIds=[2]))
db.session.commit()

api.add_resource(ProjectsResource, '/projects')
api.add_resource(ProjectResource, '/projects/<int:projectId>')
api.add_resource(ProjectUserResource, '/projects/<int:projectId>/members')

if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0")