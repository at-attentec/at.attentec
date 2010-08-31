class AddFailedAppLoginsToUsers < ActiveRecord::Migration
  def self.up
    add_column :users, :failed_app_logins, :integer
  end

  def self.down
    remove_column :users, :failed_app_logins
  end
end
