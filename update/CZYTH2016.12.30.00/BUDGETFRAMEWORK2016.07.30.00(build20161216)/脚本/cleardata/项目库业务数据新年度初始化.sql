---资金表
delete gfm_ps_pfs  where 1=1;
delete gfm_ps_pfs_task where 1=1;
--采购明细表
delete gfm_ps_stock where 1=1;
delete gfm_ps_stock_task where 1=1;
delete gfm_ps_stock_modulate where 1=1;
---审核意见
delete gfm_ps_audit where 1=1;
delete gfm_ps_audit_task where 1=1;
---转移支付
delete gfm_ps_modulate_tranpm where 1=1;
delete gfm_ps_modulate_tranpm_merge where 1=1;
delete gfm_ps_modulate_tranpm_pfs where 1=1;
---政府投资项目表
delete gfm_inv_contract where 1=1;
delete gfm_inv_contract_task where 1=1;
delete gfm_inv_info where 1=1;
delete gfm_inv_info_task where 1=1;
delete gfm_inv_pay_contract_detail where 1=1;
delete gfm_inv_pay_request where 1=1;
delete gfm_inv_pay_request_task where 1=1;
delete gfm_inv_investment_linkman where 1=1;
delete gfm_inv_ps_pfs_task where 1=1;
delete gfm_inv_relation_out where 1=1;
---上年执行情况表
delete GFM_PS_EXECUTE where 1=1;
delete GFM_PS_EXECUTE_TASK where 1=1;
delete GFM_PS_EXECUTE_DETAIL where 1=1;
delete GFM_PS_EXECUTE_DETAIL_TASK where 1=1;
---单据表
delete gfm_ps_trans_bill where 1=1;
delete gfm_ps_declare_bill where 1=1;
delete gfm_ps_modulate_bill where 1=1;
--资金转指标接口表
delete gfm_ps_pfs_interface where 1=1;
---额度表
delete gfm_ps_pfs_balance where 1=1;
delete gfm_ps_pfs_balance_serial where 1=1;
----绩效表
delete GFM_PERFORMANCE_MAIN where 1=1;
delete GFM_PERFORMANCE_INVEST where 1=1;
delete GFM_PERFORMANCE_RESULT where 1=1;
delete GFM_PERFORMANCE_PRODUCE where 1=1;
delete GFM_PERFORMANCE_EFFECT where 1=1;

delete GFM_PERFORMANCE_MAIN_TASK where 1=1;
delete GFM_PERFORMANCE_INVEST_TASK where 1=1;
delete GFM_PERFORMANCE_RESULT_TASK where 1=1;
delete GFM_PERFORMANCE_PRODUCE_TASK where 1=1;
delete GFM_PERFORMANCE_EFFECT_TASK where 1=1;
delete gfm_ps_performance where 1=1;
delete gfm_ps_performance_task where 1=1;
--测算依据表
delete gfm_ps_basis where 1=1;
--支付计划表
delete gfm_ps_pay_plan where 1=1;
--分步实施计划
DELETE FROM GFM_PS_IMP_PLAN WHERE 1=1;
DELETE FROM Gfm_Ps_Imp_Plan_Task WHERE 1=1;
---删除项目库工作流
delete from gap_wf_task where proc_id in(select proc_id from gap_wf_process where sys_id =102) ;
delete from gap_wf_tasklog where proc_id in(select proc_id from gap_wf_process where sys_id =102);
delete from gap_wf_tasklog_his where proc_id in(select proc_id from gap_wf_process where sys_id =102) ;

--项目年度表
update gfm_ps_main_year a set a.trans_bill_id=0,a.declare_bill_id=0,a.includebudget_flag=0 where 1=1; 

commit;



