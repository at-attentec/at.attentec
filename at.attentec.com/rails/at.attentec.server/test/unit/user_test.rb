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

require 'test_helper'

class UserTest < ActiveSupport::TestCase
  # Replace this with your real tests.
  test "the truth" do
    assert true
  end
  
  self.use_instantiated_fixtures  = true
  fixtures :users
  
  def test_auth 
    #check that we can login we a valid user 
    assert_equal  @bob, User.authenticate("bob", "test")    
    #wrong username
    assert_nil    User.authenticate("nonbob", "test")
    #wrong password
    assert_nil    User.authenticate("bob", "wrongpass")
    #wrong login and pass
    assert_nil    User.authenticate("nonbob", "wrongpass")
  end


  def test_passwordchange
    # check success
    assert_equal @longbob, User.authenticate("longbob", "longtest")
    #change password
    @longbob.password = @longbob.password_confirmation = "nonbobpasswd"
    assert @longbob.save
    #new password works
    assert_equal @longbob, User.authenticate("longbob", "nonbobpasswd")
    #old pasword doesn't work anymore
    assert_nil   User.authenticate("longbob", "longtest")
    #change back again
    @longbob.password = @longbob.password_confirmation = "longtest"
    assert @longbob.save
    assert_equal @longbob, User.authenticate("longbob", "longtest")
    assert_nil   User.authenticate("longbob", "nonbobpasswd")
  end

  def test_disallowed_passwords
    #check thaat we can't create a user with any of the disallowed paswords
    u = User.new    
    u.username = "nonbob"
    u.email = "nonbob@mcbob.com"
    u.first_name = "first"
    u.last_name = "last"
    #too short
    u.password = u.password_confirmation = "tiny" 
    assert !u.save     
    assert u.errors.invalid?('password')
    #too long
    u.password = u.password_confirmation = "hugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehugehuge"
    assert !u.save     
    assert u.errors.invalid?('password')
    #empty
    u.password = u.password_confirmation = ""
    assert !u.save    
    assert u.errors.invalid?('password')
    #ok
    u.password = u.password_confirmation = "bobs_secure_password"
    assert u.save     
    assert u.errors.empty? 
  end

  def test_bad_logins
    #check we cant create a user with an invalid username
    u = User.new  
    u.password = u.password_confirmation = "bobs_secure_password"
    u.email = "okbob@mcbob.com"
    u.first_name = "first"
    u.last_name = "last"
    #too short
    u.username = "x"
    assert !u.save     
    assert u.errors.invalid?('username')
    #too long
    u.username = "hugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhugebobhug"
    assert !u.save     
    assert u.errors.invalid?('username')
    #empty
    u.username = ""
    assert !u.save
    assert u.errors.invalid?('username')
    #ok
    u.username = "okbob"
    assert u.save  
    assert u.errors.empty?
    #no email
    u.email=nil   
    assert !u.save     
    assert u.errors.invalid?('email')
    #invalid email
    u.email='notavalidemail'   
    assert !u.save     
    assert u.errors.invalid?('email')
    #ok
    u.email="validbob@mcbob.com"
    assert u.save  
    assert u.errors.empty?
  end


  def test_collision
    #check can't create new user with existing username
    u = User.new
    u.username = "existingbob"
    u.password = u.password_confirmation = "bobs_secure_password"
    assert !u.save
  end


  def test_create
    #check create works and we can authenticate after creation
    u = User.new
    u.username      = "nonexistingbob"
    u.password = u.password_confirmation = "bobs_secure_password"
    u.email="nonexistingbob@mcbob.com"  
    u.first_name = "first"
    u.last_name = "last"
    assert_not_nil u.salt
    assert u.save
    assert_equal 10, u.salt.length
    assert_equal u, User.authenticate(u.username, u.password)

    u = User.new(:username => "newbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "newbob@mcbob.com",:first_name => "first", :last_name => "last" )
    assert_not_nil u.salt
    assert_not_nil u.password
    assert_not_nil u.hashed_password
    assert u.save 
    assert_equal u, User.authenticate(u.username, u.password)

  end
=begin
def test_send_new_password
  #check user authenticates
  assert_equal  @bob, User.authenticate("bob", "test")    
  #send new password
  sent = @bob.send_new_password
  assert_not_nil sent
  #old password no longer workd
  assert_nil User.authenticate("bob", "test")
  #email sent...
  assert_equal "Your password is ...", sent.subject
  #... to bob
  assert_equal @bob.email, sent.to[0]
  assert_match Regexp.new("Your username is bob."), sent.body
  #can authenticate with the new password
  new_pass = $1 if Regexp.new("Your new password is (\\w+).") =~ sent.body 
  assert_not_nil new_pass
  assert_equal  @bob, User.authenticate("bob", new_pass)    
end
=end
  def test_rand_str
    new_pass = User.random_string(10)
    assert_not_nil new_pass
    assert_equal 10, new_pass.length
  end

  def test_sha1
    u=User.new
    u.username      = "nonexistingbob"
    u.email="nonexistingbob@mcbob.com"  
    u.salt="1000"
    u.password = u.password_confirmation = "bobs_secure_password"
    u.first_name = "first"
    u.last_name = "last"
    assert u.save   
    assert_equal 'b1d27036d59f9499d403f90e0bcf43281adaa844', u.hashed_password
    assert_equal 'b1d27036d59f9499d403f90e0bcf43281adaa844', User.encrypt("bobs_secure_password", "1000")
  end

  def test_protected_attributes
    #check attributes are protected
    u = User.new(:id=>999999, :salt=>"I-want-to-set-my-salt", :username => "badbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "badbob@mcbob.com",:first_name => "first", :last_name => "last")
    assert u.save
    assert_not_equal 999999, u.id
    assert_not_equal "I-want-to-set-my-salt", u.salt

    u.update_attributes(:id=>999999, :salt=>"I-want-to-set-my-salt", :username => "verybadbob")
    assert u.save
    assert_not_equal 999999, u.id
    assert_not_equal "I-want-to-set-my-salt", u.salt
    assert_equal "verybadbob", u.username
  end
  
  def test_app_update_location_without_coordinates
    #Test login without update coordinates
    u = User.new(:username => "badbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "badbob@mcbob.com",:first_name => "first", :last_name => "last")
    assert u.save

    u = User.new(:username => "badbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "badbob@mcbob.com",:first_name => "first", :last_name => "last")
    u.app_update_location!
    assert (not u.save)
    assert u.errors.invalid?(:latitude)
    assert u.errors.invalid?(:longitude)
  end
  
  def test_app_update_location_with_coordinates
    u = User.new(:username => "badbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "badbob@mcbob.com",:first_name => "first", :last_name => "last", :latitude => '1.0', :longitude => '2.0')
    u.app_update_location!
    assert u.save
  end
  
  def test_app_wrong_phone_key_multiple
    assert_equal nil, @bob.failed_app_logins
    assert !@bob.phone_key.blank?

    #Four times wrong and then one time right
    4.times do
      @bob.login_failed
    end
    @bob.login_failed_reset
    assert !@bob.phone_key.blank?

    #five times wrong, should reset phone_key.
    5.times do
      @bob.login_failed
    end
    assert @bob.phone_key.blank?

    #Five time more just to make sure the beef is dead.
    5.times do
      @bob.login_failed
    end
    assert @bob.phone_key.blank?

  end
  
end
