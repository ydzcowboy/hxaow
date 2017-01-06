declare 
begin
     for rs in (select * from gap_biz_type t where t.biz_type_id<10230 and t.biz_type_id>10200)
     loop 
          rs.top_org_id:=--需要修改成的top_org_id;
          insert into gap_biz_type values rs;
     end loop;
end;