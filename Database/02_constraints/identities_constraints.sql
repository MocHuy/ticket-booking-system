-- CHECK Constraints
ALTER TABLE Users
ADD CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'BANNED', 'LOCKED'));

ALTER TABLE Employees
ADD CONSTRAINT chk_emp_status CHECK (job_status IN ('ACTIVE', 'RESIGNED', 'ON_LEAVE'));

ALTER TABLE Customers
ADD CONSTRAINT chk_cust_points CHECK (loyalty_points >= 0);

ALTER TABLE Customers
ADD CONSTRAINT chk_cust_spending CHECK (lifetime_spending >= 0);

ALTER TABLE MembershipTiers
ADD CONSTRAINT chk_membership_tier_name CHECK (tier_name IN ('BRONZE', 'SILVER', 'GOLD', 'DIAMOND'));

-- Foreign Keys
ALTER TABLE Permissions
ADD CONSTRAINT fk_perm_function
FOREIGN KEY (function_id) REFERENCES Functions(function_id);

ALTER TABLE RolePermissions
ADD CONSTRAINT fk_rp_role
FOREIGN KEY (role_id) REFERENCES Roles(role_id) ON DELETE CASCADE;

ALTER TABLE RolePermissions
ADD CONSTRAINT fk_rp_perm
FOREIGN KEY (permission_id) REFERENCES Permissions(permission_id) ON DELETE CASCADE;

ALTER TABLE UserRoles
ADD CONSTRAINT fk_ur_user
FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE;

ALTER TABLE UserRoles
ADD CONSTRAINT fk_ur_role
FOREIGN KEY (role_id) REFERENCES Roles(role_id) ON DELETE CASCADE;

ALTER TABLE Customers
ADD CONSTRAINT fk_customers_users
FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE;

ALTER TABLE Customers
ADD CONSTRAINT fk_customers_tier
FOREIGN KEY (membership_tier_id) REFERENCES MembershipTiers(tier_id);

ALTER TABLE Employees
ADD CONSTRAINT fk_employees_users
FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE;

-- FIX VẤN ĐỀ 6: MembershipTiers - thêm constraint cho discount_percent và min_points
ALTER TABLE MembershipTiers
ADD CONSTRAINT chk_tier_discount CHECK (discount_percent >= 0 AND discount_percent <= 100);

ALTER TABLE MembershipTiers
ADD CONSTRAINT chk_tier_points CHECK (min_points >= 0);
