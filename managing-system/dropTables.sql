-- /usr/local/mysql/bin/mysql --user=saefauser --password=SaefaPW0! < dropTables.sql

SET FOREIGN_KEY_CHECKS = 0;

-- DROP TABLE `knowledge`.`adaptation_option`, `knowledge`.`add_instance_option`, `knowledge`.`add_instance_option_instances_to_shutdown_ids`, `knowledge`.`add_instance_option_old_instances_new_weights`, `knowledge`.`change_implementation_option`, `knowledge`.`change_implementation_option_possible_implementations`, `knowledge`.`change_load_balancer_weights_option`, `knowledge`.`change_load_balancer_weights_option_instances_to_shutdown_ids`, `knowledge`.`change_load_balancer_weights_option_new_weights`, `knowledge`.`circuit_breaker_metrics`, `knowledge`.`circuit_breaker_metrics_buffered_calls_count`, `knowledge`.`circuit_breaker_metrics_call_count`, `knowledge`.`circuit_breaker_metrics_call_duration`, `knowledge`.`circuit_breaker_metrics_call_max_duration`, `knowledge`.`circuit_breaker_metrics_slow_call_count`, `knowledge`.`http_endpoint_metrics`, `knowledge`.`http_endpoint_metrics_outcome_metrics`, `knowledge`.`instance_metrics_snapshot`, `knowledge`.`instance_metrics_snapshot_circuit_breaker_metrics`, `knowledge`.`instance_metrics_snapshot_http_metrics`, `knowledge`.`qosvalue_entity`, `knowledge`.`service_configuration`, `knowledge`.`service_configuration_circuit_breakers_configuration`, `knowledge`.`service_configuration_load_balancer_weights`, `knowledge`.`shutdown_instance_option`, `knowledge`.`shutdown_instance_option_new_weights`;

TRUNCATE `knowledge`.`adaptation_option`;
TRUNCATE `knowledge`.`add_instance_option`;
TRUNCATE `knowledge`.`add_instance_option_instances_to_shutdown_ids`;
TRUNCATE `knowledge`.`add_instance_option_old_instances_new_weights`;
TRUNCATE `knowledge`.`change_implementation_option`;
TRUNCATE `knowledge`.`change_implementation_option_possible_implementations`;
TRUNCATE `knowledge`.`change_load_balancer_weights_option`;
TRUNCATE `knowledge`.`change_load_balancer_weights_option_instances_to_shutdown_ids`;
TRUNCATE `knowledge`.`change_load_balancer_weights_option_new_weights`;
TRUNCATE `knowledge`.`circuit_breaker_metrics`;
TRUNCATE `knowledge`.`circuit_breaker_metrics_buffered_calls_count`;
TRUNCATE `knowledge`.`circuit_breaker_metrics_call_count`;
TRUNCATE `knowledge`.`circuit_breaker_metrics_call_duration`;
TRUNCATE `knowledge`.`circuit_breaker_metrics_call_max_duration`;
TRUNCATE `knowledge`.`circuit_breaker_metrics_slow_call_count`;
TRUNCATE `knowledge`.`http_endpoint_metrics`;
TRUNCATE `knowledge`.`http_endpoint_metrics_outcome_metrics`;
TRUNCATE `knowledge`.`instance_metrics_snapshot`;
TRUNCATE `knowledge`.`instance_metrics_snapshot_circuit_breaker_metrics`;
TRUNCATE `knowledge`.`instance_metrics_snapshot_http_metrics`;
TRUNCATE `knowledge`.`qosvalue_entity`;
TRUNCATE `knowledge`.`service_configuration`;
TRUNCATE `knowledge`.`service_configuration_circuit_breakers_configuration`;
TRUNCATE `knowledge`.`service_configuration_load_balancer_weights`;
TRUNCATE `knowledge`.`shutdown_instance_option`;
TRUNCATE `knowledge`.`shutdown_instance_option_new_weights`;

SET FOREIGN_KEY_CHECKS = 1;