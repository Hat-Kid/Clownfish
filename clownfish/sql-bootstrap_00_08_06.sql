USE clownfish;
ALTER TABLE `clownfish`.`cf_classcontent` 
ADD INDEX `classref` (`classref` ASC);

INSERT INTO `clownfish`.`cf_property` (`hashkey`, `value`, `nodelete`) VALUES ('email_admin', '', '1');
INSERT INTO `clownfish`.`cf_property` (`hashkey`, `value`, `nodelete`) VALUES ('accountconfirmtime', '60', '1');