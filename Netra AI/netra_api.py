#HOW TO RUN ->
# 1. host this file using -> python -m fastapi run netra_api.py --port 8001
# 2. use Cloudflare Tunnel to expose the port use -> cloudflared tunnel --url http://localhost:8001

import os
from dotenv import load_dotenv

from fastapi import FastAPI
from pydantic import BaseModel

######################------------ CHAT BOT API -------------######################

#Imports for Pinecone and Vector Store who help u store the Docs on vector database
from pinecone import Pinecone
from llama_index.vector_stores.pinecone import PineconeVectorStore
from llama_index.embeddings.gemini import GeminiEmbedding 

#Imports for Query Engine and again for Semantic Search using Vector_Store_Index
from llama_index.core import VectorStoreIndex
from llama_index.core.retrievers import VectorIndexRetriever
from llama_index.core.query_engine import RetrieverQueryEngine



from llama_index.llms.gemini import Gemini #LLM

from llama_index.core import Settings

load_dotenv()  # Load environment variables from .env file

llm = Gemini()
embed_model = GeminiEmbedding(model_name="models/embedding-001")

Settings.llm = llm
Settings.embed_model = embed_model
Settings.chunk_size = 1024 #smaller value is more precise that is after every 1024 characters it will create a new chunk

pinecone_client = Pinecone(api_key=os.environ.get("PINECONE_API_KEY"))


pinecone_index = pinecone_client.Index("chatbot6")  # The Index name
vector_store = PineconeVectorStore(pinecone_index=pinecone_index)

index = VectorStoreIndex.from_vector_store(vector_store=vector_store)
retriever = VectorIndexRetriever(index=index, similarity_top_k=5)

#Lets BUILD one Contextual Query Engine
from llama_index.core.chat_engine import ContextChatEngine
from llama_index.core.memory import ChatMemoryBuffer
from llama_index.core.chat_engine.types import ChatMessage

######################------------ SQL SHIT -------------######################
import psycopg2

DB_HOST = os.environ.get("DB_HOST")
DB_DB = os.environ.get("DB_DB")
DB_USER = os.environ.get("DB_USER")
DB_PASSWORD = os.environ.get("DB_PASSWORD")
DB_PORT = os.environ.get("DB_PORT")

curr = None
conn = None

try:

  conn = psycopg2.connect(
        host=DB_HOST,
        database=DB_DB,
        user=DB_USER,
        password=DB_PASSWORD,
        port=DB_PORT
    )

  curr = conn.cursor()

  print("Connected succesfully!")

except Exception as e:
  print(f"Error connecting to the database: {e}")
  raise e

######################------------  JUST API  ------------######################

app = FastAPI()

class User_Id(BaseModel):
    user_id: str

@app.get("/")
def default():
    return(f"end point is '/query' ")

@app.post("/query")
def get_query_from_user(user: User_Id):
    id = user.user_id
    print(f"Id passed: {id}")

    #chat_history for stroing context ->
    chat_history = []

    #lets fetch all the required stuffs ->

    # Execute the query

    try:

        create_table = """ 
            CREATE TABLE IF NOT EXISTS chat_history (
            id SERIAL PRIMARY KEY,
            user_id TEXT NOT NULL,
            role VARCHAR(10) NOT NULL,
            message TEXT NOT NULL,
            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
         ); """
        
        curr.execute(create_table)
        conn.commit()

        curr.execute("""
            SELECT *
            FROM chat_history
            WHERE user_id = %s
            ORDER BY timestamp
        """, (id,))

        chat_history = []

        # Fetch all matching rows
        rows = curr.fetchall()

        for row in rows:
            chat_history.append({
                "role": row[2],
                "content": row[3],
            })

        print("### Chat History Fetched Successfully ###")

        user_question = chat_history[-1]["content"] if chat_history else "you respond with How can i help you today?"

        memory = ChatMemoryBuffer.from_defaults()

        for msg in chat_history:
            msg = ChatMessage(role=msg['role'], content=msg["content"])
            memory.put(msg)

        #Contextual Chat Engine

        # System prompt identity

        identity_prompt = """
        You are Netra, a chatbot designed to assist users during hazards and hazard-like situations.
        Your primary objectives are to help prevent hazards, provide first aid guidance, and assist in hazard.
        Be Respectful, helpful, and concise in your responses. and no need to bold oer italicize the text.

        Always respond to only hazrad related quesitons and reply with i cant help with non-hazard related questions if you see any non related questions. try to use emojis in your response to make it more engaging and friendly. 

        You are Netra(RAG Chabot) built by Rajat Nandkumar Shedshyal. No relation strictly with Google or any other company. all the other very improtant information is below, and also give focus to user's query. For Hazard IQ+ App, you are the main chatbot that helps users with hazard-related queries.

        Remember what features we have and tell what the user can use, if not a serious hazard tell that we will integrate it in futurre so that we do the tasks automatically. 

        if hazard make sure you provide the helpline numbers and links if required.

        No need to introduce yourself everyitme, if there is a hazard related question, just answer it directly.(with helpline numbers and links if required)

        Chatbot Name: Netra
        Version: 1.0.0

        Purpose:
        Netra is designed to assist users during hazards and hazard-like situations. 
        Its primary objectives are to help prevent hazards, provide first aid guidance, 
        and assist in hazard rescue operations. Netra acts as a reliable companion 
        in critical moments, offering timely information and instructions to ensure safety.

        Netra Chatbot was built by Rajat Nandkumar Shedshyal.
        Disaster Classifier was built by Rajat Nandkumar Shedshyal.
        API Predicion was built by Arnav Tripathi.
        Disaster Classifier was built by Arnav Tripathi.
        Application was built by Samarth Agarwal.
        Application was built by Sayantan Sen.
        
        HazardIQ+ App was built By:
        - Rajat Nandkumar Shedshyal (Machine Learning Engineer)
        - Samarth Agarwal (Backend and Frontend App Developer)
        - Sayantan Sen (Frontend App Developer)
        - Arnav Tripathi (Machine Learning Engineer)

        Year of Creation: July 2025

        Main Capabilities:
        1. Provide real-time information on ongoing hazards and recommended safety measures.
        2. Offer first aid instructions based on the type of injury or hazard.
        3. Guide users through basic hazard rescue steps while waiting for professional help.
        4. Educate users on hazard prevention and preparedness strategies.
        5. Retrieve relevant hazard-related documents and resources from a connected knowledge base.
        6. Provide SOS real time help, using SOS button send real time location through notifications, Chat option between responders and users
        7. Maps the hazards, has room /group chat option to interact with at time of hazard
        8. AQI Prediction of your current city
        9. Current AQI of major cities and map them with different colours depending on danger
        10. Notifies if AQI is higher than a certain limit or if u enter danger zone
        11. To upload disaster, just take a photo and model classifies and if a disaster then gets posted automatically or rejected
        12. Netra API a chatbot to help covers the first 5 points
        13. Details about speed, whether, rain, cloudiness

        Limitations:
        - Netra does not replace professional medical or emergency services.
        - Effectiveness depends on the accuracy and completeness of the information provided by the user.
        - May not function optimally in areas with poor internet connectivity.
        - Responses are based on pre-trained and retrieved knowledge; real-time updates depend on connected data sources.

        Disclaimer:
        Netra is an assistive tool. Always follow the guidance of trained professionals 
        and official emergency services in life-threatening situations

        """

        chat_engine = ContextChatEngine.from_defaults(
            retriever=retriever,
            llm=llm,
            memory=memory,
            system_prompt=identity_prompt
        )

        # Single Convo

        import time
        start_time = time.time()
        response = chat_engine.chat(user_question)
        end_time = time.time()

        # put back the response to the database

        insert_data = """
          INSERT INTO chat_history (user_id, role, message)
          VALUES (%s, %s, %s)
        """

        # REMOVED AS APP SENDS BACK THE RESPONE ITSELF
        answer = response.response.strip()
        curr.execute(insert_data, (user.user_id, "assistant", response.response.strip()))

        conn.commit()

        print(f"Time taken: {end_time - start_time} secs,\n Bot: {answer}")
        return {
            "response": answer
        }

        ## reput the chat history back to the database

    except Exception as e:
        print(f"Error fetching data from the database: {e}")
        return {"error": "Error: {e}, maybe wrong user_id?"}



