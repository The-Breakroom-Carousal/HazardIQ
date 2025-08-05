import gradio as gr
import joblib
import numpy as np

model = joblib.load("Med_Recommender_Cat_final.pkl")

classes = [
    '(vertigo) Paroymsal  Positional Vertigo', 'AIDS', 'Acne',
    'Alcoholic hepatitis', 'Allergy', 'Arthritis', 'Bronchial Asthma',
    'Cervical spondylosis', 'Chicken pox', 'Chronic cholestasis',
    'Common Cold', 'Dengue', 'Diabetes ', 'Dimorphic hemmorhoids(piles)',
    'Drug Reaction', 'Fungal infection', 'GERD', 'Gastroenteritis',
    'Heart attack', 'Hepatitis B', 'Hepatitis C', 'Hepatitis D',
    'Hepatitis E', 'Hypertension ', 'Hyperthyroidism', 'Hypoglycemia',
    'Hypothyroidism', 'Impetigo', 'Jaundice', 'Malaria', 'Migraine',
    'Osteoarthristis', 'Paralysis (brain hemorrhage)',
    'Peptic ulcer diseae', 'Pneumonia', 'Psoriasis', 'Tuberculosis',
    'Typhoid', 'Urinary tract infection', 'Varicose veins', 'hepatitis A'
]

feature_names = [
    'muscle_pain', 'fatigue', 'mild_fever', 'high_fever', 'loss_of_appetite',
    'nausea', 'vomiting', 'itching', 'yellowing_of_eyes', 'headache',
    'chest_pain', 'joint_pain', 'dark_urine', 'yellowish_skin',
    'breathlessness', 'malaise', 'diarrhoea', 'family_history'
]

def predict_disease(*inputs):
    input_array = np.array(inputs).reshape(1, -1)
    prediction = model.predict(input_array)
    return f"Predicted Disease: {prediction[0]}"

checkbox_inputs = [gr.Checkbox(label=feature.replace('_', ' ').capitalize()) for feature in feature_names]

iface = gr.Interface(
    fn=predict_disease,
    inputs=checkbox_inputs,
    outputs="text",
    title="Medical Disease Recommender",
    description="Select symptoms to predict the most likely disease."
)

if __name__ == "__main__":
    iface.launch()