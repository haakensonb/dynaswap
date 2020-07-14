import os
import hashlib
from hashlib import md5
from DynaSwapApp.services.atallah_interface.atallah import hash_fun, encrypt, decrypt

from DynaSwapApp.models import Roles, Privileges, RolesPrivileges

"""
Notes about notation:
public:
    l_i: node label
    y_ij: edge label
private:
    s_i: random secret value
    t_i: derive key
    k_i: decrypt key
    r_ij: edge seed
"""

class Node:
    def __init__(self, name):
        """
        Constructor for node. Will use urandom and md5 hash to generate node
        label (l_i) and secret value (s_i). Each node contains a list of all
        the edges to it's child nodes.
        Args:
            name (string): name to identify node
            users (list): list of users assigned to node at initialization
        Returns:
            N/A
        """
        self.name = name
        self.l_i = md5(os.urandom(16)).hexdigest()
        self.__s_i = md5(os.urandom(16)).hexdigest()
        self.edges = {}

    def update_secret(self):
        self.__s_i = md5(os.urandom(16)).hexdigest()

    def get_t_i(self):
        """
        Returns the value of the derive key (t_i).
        Args:
            N/A
        Returns:
            hex digest (string): hash of s_i + "0" + l_i
        """
        # t_i = hash_fun(s_i||0||l_i)
        return hash_fun(self.__s_i, self.l_i, val_opt="0")

    def get_k_i(self):
        """
        Returns the value of the decrypt key (k_i).
        Args:
            N/A
        Returns:
            hex digest (string): hash of s_i + "1" + l_i
        """
        # k_i = hash_fun(s_i||1||l_i)
        return hash_fun(self.__s_i, self.l_i, val_opt="1")
    
    def get_s_i(self):
        return self.__s_i


class Edge:
    def __init__(self, t_i, l_j, t_j, k_j):
        """
        Constructor for edge. Given the parent derive key, child label, child
        derive key and child decrypt key the edge will calculate the edge seed
        and the edge label.
        Args:
            t_i (string): hex string of parent derive key
            l_j (string): hex string of child label
            t_j (string): hex string of child dervive key
            k_j (string): hex string of child decrypt key
        Returns:
            N/A
        """
        # r_ij = hash_fun(t_i||l_j)
        self.__r_ij = hash_fun(t_i, l_j)
        # y_ij = AES.encrypt{r_ij}(t_j||k_j)
        self.y_ij = encrypt(self.__r_ij, t_j, k_j)

    def update_r_ij(self, t_i, l_j):
        self.r_ij = hash_fun(t_i, l_j)

    def update_y_ij(self, t_j, k_j):
        self.y_ij = encrypt(self.__r_ij, t_j, k_j)
    
    def get_r_ij(self):
        return self.__r_ij


class DAG():
    def __init__(self):
        self.priv_list = dict()
        for priv in RolesPrivileges.all():
            if priv.role not in self.priv_list.keys():
                self.priv_list[priv.role] = [priv.privilege]
            else:
                self.priv_list[priv.role].append(priv.privilege)

    def create_sketch(self):
        
        #name is list of role names, node is list of lists of privileges, node_list is dict of node objects with atallah's keys
        node = []
        node_name = []
        # name = []
        node_list = {}

        #load actual role-privilege mappings
        for role in self.priv_list:
            node_name.append([role, set(self.priv_list[role])])
            node.append(set(self.priv_list[role]))
            # name.append(role)
        
        #find all interceptions between privileges and generate dummy nodes
        d = 0
        for i in range(len(node)):
            for j in range(i + 1, len(node)):
                interc = node[i] & node[j]
                if(len(interc) < min(len(node[i]), len(node[j])) and len(interc) > 0 and interc not in node):
                    dummy_name = 'Placeholder' + str(d)
                    node_name.append([dummy_name,interc])
                    # name.append(dummy_name)
                    node.append(interc)
                    d += 1

        node.sort(key=len, reverse=True)
        name = ["" for i in range(len(node))]
        for i in range(len(node)):
            for j in range(len(node)):
                if(node_name[i][1] == node[j]):
                    name[j] = node_name[i][0]
        adj_mat = [[] for i in range(len(node))]
        tot = [set() for i in range(len(node))]

        self.dfs(adj_mat, node, tot, 0)

        node_list = {}
        # ListPrivilege.objects.all().delete()
        for i in range(len(node)):
            node_list[name[i]] = Node(name[i])
            # print(node_list[name[i]].s_i)
            # for priv in node[i]:
                # ListPrivilege(role=name[i], priv=priv).save()
        
        for i, row in enumerate(adj_mat):
            for j, val in enumerate(row):
                paren = node_list[name[val]]
                child = node_list[name[val]]
                node_list[name[i]].edges[name[val]] = Edge(
                    paren.get_t_i(), child.l_i, child.get_t_i(), child.get_k_i())

        # print(list(node_list.items())[0].s_i)
        #save to database
        # CrypRole.objects.all().delete()
        # for node_name, node_obj in node_list.items():
        #     parent = CrypRole.objects.filter(role=node_name)
        #     if not parent:
        #         CrypRole(role=node_name, label=node_obj.l_i, secret=node_obj.get_s_i()).save()
        #     parent_role = CrypRole.objects.get(role=node_name)
        #     for edge_name, edge_obj in node_obj.edges.items():
        #         child = CrypRole.objects.filter(role=edge_name)
        #         if not child:
        #             CrypRole(role=edge_name, label=node_list[edge_name].l_i, secret=node_list[edge_name].get_s_i()).save()
        #         child_role = CrypRole.objects.get(role=edge_name)
        #         RoleEdges(parent=parent_role, child=child_role, label=edge_obj.y_ij, secret=edge_obj.get_r_ij()).save()

        return adj_mat, node, name
    
    def get_formatted_graph(self, adj_mat, node, node_names):
        # use format as specified in cytoscape-dagre
        formatted_info = {
            'elements': {
                'nodes': [],
                'edges': []
            }
        }
        for i, row in enumerate(adj_mat):
            # create node object
            formatted_info['elements']['nodes'].append({
                    'data': {
                        'id': f"{node_names[i]}",
                        'label': f"{node_names[i]}"
                    }
            })
            for j, val in enumerate(row):
                # create edge object
                formatted_info['elements']['edges'].append({
                    'data': {
                        'id': f"e{i}{val}",
                        'source': f"{node_names[i]}",
                        'target': f"{node_names[val]}"
                    }
                })
        return formatted_info


    def dfs(self, adj_mat, node, tot, cur):
        if(cur == len(node)):
            return
        if(len(tot[cur]) > 0):
            self.dfs(adj_mat, node, tot, cur + 1)
            return
        
        for i in range(cur + 1, len(node)):
            self.dfs(adj_mat, node, tot, i)
            if(tot[i].issubset(node[cur]) and (not tot[i].issubset(tot[cur]))):
                if(i not in adj_mat[cur]):
                    adj_mat[cur].append(i)
                tot[cur] = set.union(tot[cur],tot[i])
        
        node[cur] = node[cur] - tot[cur]
        tot[cur] = set.union(tot[cur],node[cur])