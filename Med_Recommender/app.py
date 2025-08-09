import joblib
import pandas as pd
import gradio as gr

model = joblib.load('Med_Recommender.pkl')

desc_df = pd.read_csv('description.csv')      
diets_df = pd.read_csv('diets.csv')             
meds_df = pd.read_csv('medications.csv')        
prec_df = pd.read_csv('precautions_df.csv')     
workouts_df = pd.read_csv('workout_df.csv')     

disease_to_index = {
    '(vertigo) Paroymsal  Positional Vertigo': 0, 'AIDS': 1, 'Acne': 2,
    'Alcoholic hepatitis': 3, 'Allergy': 4, 'Arthritis': 5, 'Bronchial Asthma': 6,
    'Cervical spondylosis': 7, 'Chicken pox': 8, 'Chronic cholestasis': 9,
    'Common Cold': 10, 'Dengue': 11, 'Diabetes ': 12,
    'Dimorphic hemmorhoids(piles)': 13, 'Drug Reaction': 14,
    'Fungal infection': 15, 'GERD': 16, 'Gastroenteritis': 17, 'Heart attack': 18,
    'Hepatitis B': 19, 'Hepatitis C': 20, 'Hepatitis D': 21, 'Hepatitis E': 22,
    'Hypertension ': 23, 'Hyperthyroidism': 24, 'Hypoglycemia': 25,
    'Hypothyroidism': 26, 'Impetigo': 27, 'Jaundice': 28, 'Malaria': 29,
    'Migraine': 30, 'Osteoarthristis': 31, 'Paralysis (brain hemorrhage)': 32,
    'Peptic ulcer diseae': 33, 'Pneumonia': 34, 'Psoriasis': 35, 'Tuberculosis': 36,
    'Typhoid': 37, 'Urinary tract infection': 38, 'Varicose veins': 39, 'hepatitis A': 40
}

index_to_disease = {v: k for k, v in disease_to_index.items()}

symptom_features = ['sinus_pressure', 'back_pain', 'excessive_hunger', 'swollen_blood_vessels',
                    'depression', 'restlessness', 'irritability', 'blurred_and_distorted_vision',
                    'mild_fever', 'stomach_bleeding', 'fluid_overload.1', 'nodal_skin_eruptions',
                    'phlegm', 'muscle_weakness', 'red_spots_over_body', 'rusty_sputum',
                    'visual_disturbances', 'receiving_blood_transfusion', 'pain_behind_the_eyes',
                    'swelled_lymph_nodes', 'weakness_in_limbs', 'abnormal_menstruation', 'acidity',
                    'muscle_pain', 'stiff_neck', 'anxiety', 'blood_in_sputum', 'bruising',
                    'movement_stiffness', 'weight_loss', 'skin_peeling', 'slurred_speech', 'knee_pain',
                    'palpitations', 'indigestion', 'neck_pain', 'silver_like_dusting', 'yellow_urine',
                    'dizziness', 'throat_irritation', 'fast_heart_rate', 'internal_itching',
                    'puffy_face_and_eyes', 'mood_swings', 'belly_pain', 'small_dents_in_nails',
                    'spinning_movements', 'painful_walking', 'toxic_look_(typhos)', 'polyuria']

def prettify_feature(name):
    name = name.replace('_', ' ').replace('.1', '')
    return name.title()

def get_info(df, disease_name, columns, disease_col='Disease'):
    row = df[df[disease_col].str.lower() == disease_name.lower()]
    if row.empty:
        return "Information not available."
    info_pieces = []
    for col in columns:
        if col in df.columns:
            val = row.iloc[0][col]
            if pd.notna(val) and str(val).strip() != "":
                info_pieces.append(str(val))
    return " | ".join(info_pieces) if info_pieces else "Information not available."

def format_list_text(text):
    if text.startswith('[') and text.endswith(']'):
        items = [item.strip().strip("'\"") for item in text[1:-1].split(',')]
        return "\n".join(f"• {item}" for item in items if item)
    return text

def predict_disease(*symptom_vals):
    input_vector = list(symptom_vals)
    pred_idx = model.predict([input_vector])[0]
    disease_name = index_to_disease.get(pred_idx, "Unknown disease")

    description = get_info(desc_df, disease_name, ['Description'], disease_col='Disease')
    diet = get_info(diets_df, disease_name, ['Diet'], disease_col='Disease')
    medication = get_info(meds_df, disease_name, ['Medication'], disease_col='Disease')
    precaution = get_info(prec_df, disease_name, ['Precaution_1', 'Precaution_2', 'Precaution_3', 'Precaution_4'], disease_col='Disease')
    workout = get_info(workouts_df, disease_name, ['workout'], disease_col='disease')

    diet = format_list_text(diet)
    medication = format_list_text(medication)
    precaution_lines = precaution.replace(" | ", "\n• ")

    result = (
        f"Predicted Disease:\n{disease_name}\n\n"
        f"Description:\n{description}\n\n"
        f"Diet to be taken:\n{diet}\n\n"
        f"Medications to be taken:\n{medication}\n\n"
        f"Precautions to take:\n• {precaution_lines}\n\n"
        f"Workouts to do:\n{workout if workout != 'Information not available.' else 'No specific workouts available.'}"
    )
    return result

inputs = [gr.Checkbox(label=prettify_feature(s)) for s in symptom_features]
output = gr.Textbox(label="Prediction and Details")

gr.Interface(fn=predict_disease, inputs=inputs, outputs=output,
             title="Disease Predictor",
             description="Check your symptoms by selecting the checkboxes and get disease prediction with details."
             ).launch()