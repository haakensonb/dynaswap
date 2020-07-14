"""  DynaSwapApp/models.py  """
from django.db import models, connection
from collections import namedtuple


class Roles(models.Model):
    """  OpenMRS role Class  """
    class Meta:
        managed = False
        db_table = "role"
    role = models.CharField(max_length=50, unique=True, primary_key=True)
    description = models.CharField(max_length=255, default='')
    uuid = models.CharField(max_length=38)
    url = models.URLField(max_length=255)
    feature = models.BinaryField()

    def __str__(self):
        return self.role


class Privileges(models.Model):
    """ OpenMRS privilege table Class 
        Possible privileges that can be assigned in OpenMRS 
    """
    class Meta:
        managed = False
        db_table = "privilege"
    privilege = models.CharField(max_length=255, unique=True, primary_key=True)
    description = models.TextField()
    uuid = models.CharField(max_length=38)

    def __str__(self):
        return self.privilege


class RolesPrivileges():
    """
    Django ORM doesn't support tables with composite keys like 'role_privilege'.
    Instead, custom SQL queries will be used and grouped together as static methods on this class.
    """
    @staticmethod
    def all():
        """
        Will return the names of the role/priv mapping but will not return
        the associated role/priv object.
        """
        with connection.cursor() as cursor:
            sql = "SELECT role, privilege FROM role_privilege"
            cursor.execute(sql)
            row = namedtuplefetchall(cursor)

        return row


def namedtuplefetchall(cursor):
    """ Return all rows from a cursor as a namedtuple """
    desc = cursor.description
    nt_result = namedtuple('Result', [col[0] for col in desc])
    return [nt_result(*row) for row in cursor.fetchall()]


class Users(models.Model):
    """  OpenMRS users Class  """
    class Meta:
        managed = False
        db_table = "users"
    user_id = models.IntegerField(unique=True, primary_key=True)
    username = models.CharField(max_length=50, unique=True)


class DynaSwapUsers(models.Model):
    """  dynaswap_users Class  """
    class Meta:
        # This table is currently created using the OpenMRS liquibase xml file
        # so 'managed' must be set to false to prevent attempting to create it twice.
        managed = False
        db_table = "dynaswap_users"
    face_authentication_id = models.IntegerField(primary_key=True)
    user_id = models.IntegerField()
    role = models.CharField(max_length=50)
    bio_capsule = models.BinaryField()
    classifier = models.BinaryField()
    created_on = models.DateTimeField(auto_now=True)
    last_authenticated = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.face_authentication_id, self.user_id, self.role


class UsersRoles(models.Model):
    """ OpenMRS user_role Class  
        The Django ORM doesn't support a composite key design. This model doesn't
        actually work and will need to be replaced with a custom SQL queries.
    """
    class Meta:
        managed = False
        db_table = "user_role"
    user_id = models.ForeignKey(
        Users, db_column="user_id", on_delete=models.CASCADE)
    role = models.ForeignKey(Roles, db_column="role", on_delete=models.CASCADE)

    def __str__(self):
        return self.user_id, self.role
