class AddStatusToUser < ActiveRecord::Migration
  def self.up
    change_table :users do |t|
      t.string :status
      t.string :status_custom_message
    end
  end

  def self.down
    change_table :users do |t|
      t.remove :status
      t.remove :status_custom_message
    end
  end
end
