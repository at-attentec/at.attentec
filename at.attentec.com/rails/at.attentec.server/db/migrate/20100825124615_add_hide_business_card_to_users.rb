class AddHideBusinessCardToUsers < ActiveRecord::Migration
  def self.up
    add_column :users, :hide_business_card, :boolean
  end

  def self.down
    remove_column :users, :hide_business_card
  end
end
