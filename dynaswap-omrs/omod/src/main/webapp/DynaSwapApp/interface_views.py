from django.shortcuts import render, redirect
from interface.models import Role, PossiblePrivilege, Privilege
from django.http import JsonResponse
from interface.services.priv import DAG
import json


# Create your views here.
def index(request):
    possible_privs = PossiblePrivilege.objects.all()
    roles = Role.objects.all()
    return render(request, 'interface/index.html', context={'possible_privs': possible_privs, 'roles': roles})


def roles_and_privs(request):
    # get_all_roles_with_privs is a static method on Role model
    data_list = Role.get_all_roles_with_privs()
    return JsonResponse({'context': data_list})


def all_possible_privs(request):
    possible_privs = list(PossiblePrivilege.objects.all().values())
    return JsonResponse({'context': possible_privs})


def add_possible_priv(request):
    if request.method == 'POST':
        json_data = json.loads(request.body.decode("utf-8"))
        priv = json_data['add_priv']
        obj, created = PossiblePrivilege.objects.get_or_create(
            priv_name=priv
        )
        outgoing_data = {}
        if created:
            outgoing_data['message'] = 'Created possible privilege'
            # send back the priv that was added so the frontend knows what to add
            outgoing_data['added_priv'] = priv
        elif obj:
            outgoing_data['message'] = f"possible privilege {obj.priv_name} already exists"
        return JsonResponse({'context': outgoing_data})
    return JsonResponse({'error': 'Request method was not POST'})


def delete_possible_priv(request):
    if request.method == 'POST':
        json_data = json.loads(request.body.decode('utf-8'))
        delete_privs = json_data['delete_privs']
        data = {'message': "Deleted privilege(s): ", 'error': "Error privileges do not exist: "}
        for priv in delete_privs:
            try:
                p = PossiblePrivilege.objects.get(priv_name=priv)
                p.delete()
                data['message'] += priv
            except PossiblePrivilege.DoesNotExist:
                data['error'] += priv
        return JsonResponse({'context': data})
    return JsonResponse({'error': 'Request method was not POST'})


def add_role(request):
    if request.method == 'POST':
        json_data = json.loads(request.body.decode('utf-8'))
        role = json_data['role']
        obj, created = Role.objects.get_or_create(
            role_name=role
        )
        data = {'message': ""}
        if created:
            data['message'] = f"created role {role}"
            data['role'] = Role.get_role_with_privs(role)
        elif obj:
            data['error'] = f"role {role} already exists"
        return JsonResponse({'context': data})
    return JsonResponse({'error': 'Request method was not POST'})


def delete_role(request):
    if request.method == 'POST':
        json_data = json.loads(request.body.decode('utf-8'))
        roles = json_data['roles']
        data = {'message': "Deleted role(s): ", 'error': "Error roles do not exist: "}
        for role in roles:
            try:
                r = Role.objects.get(role_name=role)
                r.delete()
                data['message'] += role
            except Role.DoesNotExist:
                data['error'] += role
    return JsonResponse({'error': 'Request method was not POST'})


def get_role_privs(request, role):
    r = Role.get_role_with_privs(role)
    p = list(PossiblePrivilege.objects.all().values())
    data = {
        'role': r,
        'possible_privs': p
    }
    return JsonResponse({'context': data})


def edit_role_priv_assignment(request):
    if request.method == 'POST':
        json_data = json.loads(request.body.decode('utf-8'))
        role_name = json_data['role']
        selected_privs = json_data['selected_privs']
        not_selected_privs = json_data['not_selected_privs']
        r = Role.objects.get(role_name=role_name)
        curr_privs = [priv.priv.priv_name for priv in r.privilege_set.all()]
        data = {'message': "", 'error': ""}

        for selected_priv in selected_privs:
            if selected_priv not in curr_privs:
                # add privilige
                p = PossiblePrivilege.objects.get(priv_name=selected_priv)
                # need to add check to make sure role and privilige are found
                obj, created = Privilege.objects.get_or_create(
                    role=r,
                    priv=p
                )
                if created:
                    data['message'] += f"Assigned privilege {selected_priv} to {role_name}\n"
                elif obj:
                    data['error'] += f"Role {role_name} already has privilege {selected_priv}\n"

        for not_selected_priv in not_selected_privs:
            if not_selected_priv in curr_privs:
                # remove privilge
                p = PossiblePrivilege.objects.get(priv_name=not_selected_priv)
                try:
                    privilege = Privilege.objects.get(role=r, priv=p)
                    privilege.delete()
                    data['message'] += f"Removed privilege {not_selected_priv} for role {role_name}\n"
                except:
                    data['error'] += f"Could not remove privilege {not_selected_priv} from role {role_name}\n"

        return JsonResponse({'context': data})
    return JsonResponse({'error': 'Request method was not POST'})


def assign_priv_to_role(request):
    role = request.POST.get('role')
    priv = request.POST.get('priv')
    r, p = None, None
    try:
        r = Role.objects.get(role_name=role)
    except Role.DoesNotExist:
        print("role does not exist")

    try:
        p = PossiblePrivilege.objects.get(priv_name=priv)
    except PossiblePrivilege.DoesNotExist:
        print("privilege does not exist")

    if r and p:
        obj, created = Privilege.objects.get_or_create(
            role=r,
            priv=p
        )
        if created:
            print(f"assigned privilege {priv} to {role}")
        elif obj:
            print(f"role {role} already has privilege {priv}")

    return redirect('index')


def remove_priv_from_role(request):
    role = request.POST.get('role')
    priv = request.POST.get('priv')
    r, p = None, None
    try:
        r = Role.objects.get(role_name=role)
    except Role.DoesNotExist:
        print("role does not exist")

    try:
        p = PossiblePrivilege.objects.get(priv_name=priv)
    except PossiblePrivilege.DoesNotExist:
        print("privilege does not exist")

    if r and p:
        try:
            privilege = Privilege.objects.get(role=r, priv=p)
            privilege.delete()
            print(f"removed privilege {priv} for role {role}")
        except:
            print("could not remove privilege")
    return redirect('index')


def create_dag(request):
    try:
        dag = DAG()
        adj_mat, nodes, name = dag.create_sketch()
        formatted_graph = dag.get_formatted_graph(adj_mat, nodes, name)
        return JsonResponse({'message': 'DAG created', 'formatted_graph': formatted_graph})
    except Exception as e:
        print(e)
        return JsonResponse({'error': 'Something went wrong creating the DAG'})