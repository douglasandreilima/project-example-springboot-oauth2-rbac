package com.oauth2.config.auth.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.base.Splitter;
import com.oauth2.entities.Permission;
import com.oauth2.entities.Role;
import com.oauth2.entities.User;
import com.oauth2.services.IUserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

	@Autowired
	private IUserService userService;
	
	@Override
	public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
		
		if ((auth == null) || (permission == null)){
            return false;
        }
		
		try {
			List<String> permissionsValid = validPermissions(auth, permission);

			//Valid
			if(!permissionsValid.isEmpty()) {
				log.trace("Permission Valid for this method");
				return true;
			}else {
				log.trace("Permission Invalid for this method");
				return false;
			}
		} catch (Exception e) {
			log.error("Error in method hasPermission in class CustomPermissionEvaluator: ", e);
			return false;
		}
	}

	@Override
	public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
		return true;
	}
	
	private List<String> validPermissions(Authentication auth, Object permission) {
		log.debug("Begin - validating user permission in method validPermissions in class CustomPermissionEvaluator");
		
		UUID uuidUser = UUID.fromString(auth.getPrincipal().toString());
		
		List<String> permissionsMethod = Splitter.on(',')
				.trimResults()
				.omitEmptyStrings()
				.splitToList(permission.toString().substring(1, permission.toString().length()-1));
		
		List<String> permissionsValid = new ArrayList<>();
		
		if (permissionsMethod.get(0).equals("roles")) {
			permissionsValid = validWithRoles(uuidUser, permissionsMethod);
		} 
		
		if (permissionsMethod.get(0).equals("permissions")) {
			permissionsValid = validWithPermissions(uuidUser, permissionsMethod);
		}
		
		log.debug("End - validating user permission in method validPermissions in class CustomPermissionEvaluator");
		return permissionsValid;
	}

	
	
	private List<String> validWithRoles(UUID uuidUser, List<String> rolesMethod) {
		
		User user = userService.findByUuid(uuidUser)
				.orElseThrow(() -> new UsernameNotFoundException("Error -> hasPermission for UUID: " + uuidUser));
		
		List<String> rolesUser = new ArrayList<>();
		
		for (Role r : user.getRoles()) {
			rolesUser.add(r.getName());
		}
		
		List<String> permissionsValid = rolesMethod.stream()
				.filter(p -> rolesUser.contains(p))
				.collect(Collectors.toList());
		return permissionsValid;
	}
	
	
	private List<String> validWithPermissions(UUID uuidUser, List<String> permissionsMethod) {
		
		User user = userService.findByUuid(uuidUser)
				.orElseThrow(() -> new UsernameNotFoundException("Error -> hasPermission for UUID: " + uuidUser));
		
		List<String> permissionsUser = new ArrayList<>();
		
		for (Role r : user.getRoles()) {
			for (Permission p : r.getPermissions()) {
				permissionsUser.add(p.getName());
			}
		}
		
		List<String> permissionsValid = permissionsMethod.stream()
				.filter(p -> permissionsUser.contains(p))
				.collect(Collectors.toList());
		return permissionsValid;
	}
	
}

/*
Exemplos:
OR -> @PreAuthorize("hasPermission(returnObject, {'user_create', 'user_update', 'abcd_create', 'abcd_read', 'user_read'})")
AND -> @PreAuthorize("hasPermission(returnObject, {'user_read'}) AND hasPermission(returnObject, {'user_update'})")
@PreAuthorize("hasPermission(#id, 'Foo', 'read')")
 */
