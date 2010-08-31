class AddAdministratorToUsers < ActiveRecord::Migration
  def self.up
    change_table :Users do |t|
      t.boolean :administrator
    end
  end

  def self.down
    change_table :Users do |t|
      t.remove :administrator
    end
  end
end
