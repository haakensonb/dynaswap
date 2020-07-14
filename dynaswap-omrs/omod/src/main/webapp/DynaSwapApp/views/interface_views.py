from django.shortcuts import render
from django.http import JsonResponse
from DynaSwapApp.services.atallah_interface.dag import DAG
import json

def index(request):
    return render(request, 'interface.html')

def create_dag(request):
    try:
        dag = DAG()
        adj_mat, nodes, name = dag.create_sketch()
        formatted_graph = dag.get_formatted_graph(adj_mat, nodes, name)
        return JsonResponse({'message': 'DAG created', 'formatted_graph': formatted_graph})
    except Exception as e:
        print(e)
        return JsonResponse({'error': 'Something went wrong creating the DAG'})