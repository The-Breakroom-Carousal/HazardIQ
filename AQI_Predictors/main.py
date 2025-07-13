from fastapi import FastAPI
from pydantic import BaseModel
from model_utils import predict_aqi

app = FastAPI()

class AQIInput(BaseModel):
    state: str
    features: dict

@app.get("/")
def root():
    return {"message": "Welcome to AQI predictor API"}

@app.post("/predict")
def predict(input_data: AQIInput):
    try:
        results = predict_aqi(input_data.state, input_data.features)
        return {"state": input_data.state, "prediction": results}
    except Exception as e:
        return {"error": str(e)}