import os
from dotenv import load_dotenv
from pinecone import Pinecone

from llama_index.llms.gemini import Gemini #LLM
from llama_index.embeddings.gemini import GeminiEmbedding #Embedding

from llama_index.core import Settings

load_dotenv()  # Load environment variables from .env file

llm = Gemini()
embed_model = GeminiEmbedding(model_name="models/embedding-001")

Settings.llm = llm
Settings.embed_model = embed_model
Settings.chunk_size = 1024 #smaller value is more precise that is after every 1024 characters it will create a new chunk

pinecone_client = Pinecone(api_key=os.environ.get("PINECONE_API_KEY"))

print(f"Printing Indexes in Pinecone:")
for index in pinecone_client.list_indexes(): #Gonna loop through all the indexes in Pinecone
  print(f"Index: {index['name']}")
  

from llama_index.core import SimpleDirectoryReader

# Point to the folder where your .txt file is stored
documents = SimpleDirectoryReader("./data").load_data()

#now lets break these documents into chunks and then use a embreding model and store in VectorStore(pin
# econe)

from llama_index.core.ingestion import IngestionPipeline
from llama_index.core.node_parser import SentenceSplitter
from llama_index.vector_stores.pinecone import PineconeVectorStore

pinecone_index = pinecone_client.Index("chatbot6")  # The Index name
vector_store = PineconeVectorStore(pinecone_index=pinecone_index) 

pipeline = IngestionPipeline(
  transformations = [
    SentenceSplitter(chunk_size=1024, chunk_overlap=20),
    embed_model
  ],
  vector_store=vector_store
)


push_files = True  # Set to False if you don't want to push files again

if push_files:
  pipeline.run(documents=documents) #document is gonna pull the documents and then go through pipeline and then store in vector store
  print("Documents pushed to Pinecone successfully!")