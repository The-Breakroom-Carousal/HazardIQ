import os
import onnx
from onnx_tf.backend import prepare
import tensorflow as tf

# Optional safety
os.environ["TF_ENABLE_DEDUP_BUFFER"] = "0"  # Fix known issue
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"

# --- Step 1: Load Simplified ONNX ---
onnx_model = onnx.load("model.onnx")
print("✅ Loaded simplified ONNX model.")

# --- Step 2: Convert to TensorFlow SavedModel ---
saved_model_dir = "converted_saved_model"
tf_rep = prepare(onnx_model)
tf_rep.export_graph(saved_model_dir)
print(f"✅ Converted to TensorFlow SavedModel at: {saved_model_dir}")

# --- Step 3: Convert to TFLite ---
converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

# Enable broader op compatibility
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,
    tf.lite.OpsSet.SELECT_TF_OPS,
]

# Optional: optimize for size/speed
converter.optimizations = [tf.lite.Optimize.DEFAULT]

try:
    tflite_model = converter.convert()
    print("✅ Successfully converted to TFLite.")
except Exception as e:
    print("❌ TFLite conversion failed:")
    print(e)
    exit(1)

# --- Step 4: Save .tflite Model ---
with open("best_model.tflite", "wb") as f:
    f.write(tflite_model)
print("✅ Saved model as best_model.tflite")

# --- Step 5: Input/Output Inspection ---
interpreter = tf.lite.Interpreter(model_path="best_model.tflite")
interpreter.allocate_tensors()

print("\n🔍 Model Inputs:")
for i in interpreter.get_input_details():
    print(i)

print("\n🔍 Model Outputs:")
for o in interpreter.get_output_details():
    print(o)

print("\n✅ All Done.")
