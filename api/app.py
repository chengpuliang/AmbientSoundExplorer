from flask import Flask,jsonify,make_response
from flask_restful import Api,Resource,request
import json

music_lists = list(json.load(open("music.json","r",encoding="UTF-8")))
api_keys = ["ABC123"]

app = Flask("AmbientSoundExplorerApi")
api = Api(app)

class MusicList(Resource):
    def get(self):
        result = list(music_lists)
        sort_order = request.args.get("sort_order","ascending")
        filter_term = request.args.get("filter_term","").lower()
        if (sort_order == "descending"): result.reverse()
        result = list(filter(lambda m: filter_term in m["title"].lower(),result))
        return jsonify(result)

class MusicAudio(Resource):
    def get(self):
        music_id = request.args.get("music_id",-1,type=int)
        music=list(filter(lambda m: m["music_id"]==music_id,music_lists))
        if(not music):
            return make_response(jsonify({"detail":"Invalid music_id"}),404)

api.add_resource(MusicList, "/music/list")
api.add_resource(MusicAudio,"/music/audio")

@app.before_request
def checkKey():
    if request.headers.get("X-API-KEY","") not in api_keys:
        return jsonify({"detail":"Not authenticated" if request.headers.get("X-API-KEY","")=="" else "Invalid API key" }), 401

if __name__ == '__main__':
    app.run(port=8000,debug=True)