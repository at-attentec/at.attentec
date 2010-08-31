#
# Copyright (c) 2010 Attentec AB, http://www.attentec.se
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 

# -*- coding: utf-8 -*-
require 'digest/sha1'


class User < ActiveRecord::Base
  
  validates_presence_of :username, :salt, :first_name, :last_name 
  validates_presence_of :password, :password_confirmation, :on => :update, :if => :password_required?
  validates_presence_of :password, :password_confirmation, :on => :create 

  validates_length_of :username, :within => 3..40
  validates_format_of :username, :with => /^[\w\d]+$/
  
  validates_length_of :password, :on => :create, :within => 6..40
  validates_confirmation_of :password, :on => :create
  validates_length_of :password, :within => 6..40, :on => :update, :if => :password_required?
  validates_confirmation_of :password, :on => :update, :if => :password_required?

  validates_numericality_of :latitude, :longitude, :allow_nil => true

  validates_uniqueness_of :username, :email
  
  validates_format_of :email, :with => /^([^@\s]+)@((?:[-a-z0-9]+\.)+[a-z]{2,})$/i
  
  validates_format_of :phone, :with => /^(\+)?[0-9]*$/
 
  validates_numericality_of :zipcode, :only_integer => true, :allow_nil => true

  attr_protected :id, :salt
  
  attr_accessor :password, :password_confirmation
  
  before_validation :phone_strip
  
  validates_presence_of :longitude, :if => :app_update_location?
  validates_presence_of :latitude, :if => :app_update_location?

  validates_length_of :title, :maximum => 200, :allow_nil => true
  validates_length_of :client, :maximum => 200, :allow_nil => true
  validates_format_of :linkedin_url, :with => /^(?:http\:\/\/)?(?:[\w]+\.)?linkedin\.com\/pub\/(?:.+\/)?(?:[\w\d]+)\/(?:[\w\d]+)\/(?:[\w\d]+)$/, :if => :linkedin_url?
  validates_length_of :degree, :maximum => 200, :allow_nil => true
  
  # For paperclip file upload plugin
  # ">" means keep aspect ratio
  has_attached_file   :photo, :styles => { :businesscard => ["220x320>",:png], :android => ["48x48>",:png], :vcard => ["115x115#",:jpeg]},
                      :default_url => "/images/:attachment/missing/:style/missing.png",
                      :url  => "/images/:attachment/:id/:style/:basename.:extension",
                      :path => ":rails_root/public/images/:attachment/:id/:style/:basename.:extension"
  validates_attachment_size :photo, :less_than => 2.megabytes
  validates_attachment_content_type :photo, :content_type => ['image/jpeg', 'image/png', 'image/pjpeg', 'image/x-png'] # The last two are for dum IE compability
  
  #Status
  validates_length_of :status, :maximum => 15, :allow_nil => true
  validates_length_of :status_custom_message, :maximum => 25, :allow_nil => true
  
  validate :correct_status

  before_post_process :transliterate_file_name

  def login_failed
    self.failed_app_logins ||= 0
    self.failed_app_logins += 1
    if self.failed_app_logins >= 5
      remove_phone_key
      login_failed_reset
    end
    save!
  end
  
  def login_failed_reset
    self.failed_app_logins = 0
    save!
  end

  def correct_status
    errors.add(:status, "must be a correct status.") unless (self[:status] =~ /^(online|offline|do_not_disturb|not_available|away|invisible)$/ or self[:status].nil?)
  end

  def password=(pass)
    if pass == ''
      return
    end
    @password=pass
    self.salt = User.random_string(10) if !self.salt?
    self.hashed_password = User.encrypt(@password, self.salt)
  end

  def phone_strip
    self.phone = phone.gsub(/ /,'') if phone
  end

  before_validation :phone_strip

  def self.authenticate(username, pass)
    u=find(:first, :conditions=>["username = ?", username])
    return nil if u.nil?
    return u if User.encrypt(pass,u.salt) == u.hashed_password
    return nil
  end
  
#   def send_new_password
#     new_pass = User.random_string(10)
#     self.password = self.password_confirmation = new_pass
#     self.save
# =begin
#   Add email notification also if this ever will be used.
# =end
#   end

  def generate_phone_key
    self.phone_key = User.random_string(8)
    self.save
  end

  def remove_phone_key
    self.phone_key = ''
    self.save
  end
  
  def app_update_location!
    @app_update_location = true
  end

  def self.locations_and_status(user_id)
    t = Time.new - 3.hours
    User.all :select => 'id,latitude,longitude,location_updated_at,status,status_custom_message,connected_at', :readonly => true, :conditions => ['connected_at > ? AND id != ?', t, user_id]
  end
  
  def location_fresh?
    (latitude or longitude) and (Time.new - (location_updated_at or Time.at(0))) < 3.hours
  end

  protected
  
  def self.encrypt(pass,salt)
    Digest::SHA1.hexdigest(pass+salt)
  end
  
  def self.random_string(len)
    chars = ("a".."z").to_a + ("A".."Z").to_a + ("0".."9").to_a
    newpass = ""
    1.upto(len) do
      newpass << chars[rand(chars.size-1)]
    end
    return newpass
  end
  
  def password_required?
    !password.blank?
  end

  def as_json(options)
    begin
      self[:photo_url] = photo.url(:android)
    rescue ActiveRecord::MissingAttributeError
      #This happens when we fetch locations, no photo is fetched
    end
    super(:only => [:id, :username, :first_name, :last_name, :address, :zipcode, :city, :phone, :email, :latitude, :longitude, :location_updated_at, :title, :degree, :linkedin_url, :client, :photo_updated_at, :photo_url, :status, :status_custom_message, :connected_at])
  end

  def app_update_location?
    @app_update_location
  end

  def linkedin_url?
    !linkedin_url.blank?
  end
  
  def transliterate_file_name
    extension = File.extname(photo_file_name).gsub(/^\.+/, '')
    filename = photo_file_name.gsub(/\.#{extension}$/,'')
    self.photo.instance_write(:file_name, "#{transliterate(filename)}.#{transliterate(extension)}")
  end

  #Converts a string to only A-Za-z0-9 and -
  def transliterate(str)
    # Escape str by transliterating to UTF-8 with Iconv
    s = Iconv.iconv('ascii//ignore//translit', 'utf-8', str).to_s
    # Downcase string
    s.downcase!
    # Remove apostrophes so isn't changes to isnt
    s.gsub!(/'/, '')
    # Replace any non-letter or non-number character with a space
    s.gsub!(/[^A-Za-z0-9]+/, ' ')
    # Remove spaces from beginning and end of string
    s.strip!
    # Replace groups of spaces with single hyphen
    s.gsub!(/\ +/, '-')
    return s
  end
end
