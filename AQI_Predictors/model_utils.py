import os
import joblib
import pandas as pd
import xgboost as xgb

MODEL_DIR = "AQI_models"

def load_model(state: str, pollutant: str):
    filename = f"{state}_{pollutant}.pkl"
    path = os.path.join(MODEL_DIR, filename)

    if not os.path.exists(path):
        raise FileNotFoundError(f"Model file not found: {filename}")

    with open(path, "rb") as f:
        model = joblib.load(f)

    return model

def predict_aqi(state: str, features: dict):
    df = pd.DataFrame([features])
    # Convert city column to categorical if exists
    if "city" in df.columns:
        df["city"] = df["city"].astype("category")
    predictions = {}
    for pollutant in ["PM25", "PM10"]:
        model = load_model(state, pollutant)
        pred = model.predict(df)[0]
        predictions[pollutant] = round(float(pred), 2)
    return predictions