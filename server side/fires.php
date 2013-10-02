<?php
error_reporting(0);
$func = $_GET['function'];//test_conn,get_rows,store_row
if ($func == 'test_conn_user') {
    $db = 'firereporter';
    $user = $_GET['user'];
    $pass = $_GET['pass'];
    test_conn($db,$user,$pass);
} elseif ($func == 'test_conn_nasa') {
    $db = 'mod14';
    $user = $_GET['user'];
    $pass = $_GET['pass'];
    test_conn($db,$user,$pass);
} elseif ($func == 'get_rows_nasa') {
    $db = 'mod14';
    $table = 'spots';
    $user = $_GET['user'];
    $pass = $_GET['pass'];
    $limit = $_GET['limit'];
    $date = $_GET['date'];
    $lat = $_GET['lat'];
    $lon = $_GET['lon'];
    $radius = $_GET['radius'];
    get_rows_nasa($db, $user, $pass, $table, $lat, $lon, $radius, $date, $limit);
} elseif ($func == 'get_rows_user') {
    $db = 'firereporter';
    $table = 'fires';
    $user = $_GET['user'];
    $pass = $_GET['pass'];
    $limit = $_GET['limit'];
    $date = $_GET['date'];
    $lat = $_GET['lat'];
    $lon = $_GET['lon'];
    $radius = $_GET['radius'];
    get_rows_user($db, $user, $pass, $table, $lat, $lon, $radius, $date, $limit);
} elseif ($func == 'store_row') {
    $db = 'firereporter';
    $table = 'fires';
    $user = $_GET['user'];
    $pass = $_GET['pass'];
    $lat = $_GET['lat'];
    $lon = $_GET['lon'];
    $date = $_GET['date'];
    $dist = $_GET['dist'];
    $az = $_GET['az'];
    $comment = $_GET['comment'];
    store_row($db, $user, $pass, $table, $lat, $lon, $date, $dist, $az, $comment);
} else {
    $return['error'] = true;
    $return['msg'] = "unsupported function $func";
    echo json_encode($return);
}

function get_rows_nasa($db, $user, $pass, $table, $lat, $lon, $radius, $date, $limit){
    if($limit > 50){
        $limit = 50;
    }
    $host = 'localhost';
    $port = 5432;
    $adds_degree = 5; 

    try 
    {
        $conn = pg_connect("host=$host port=$port dbname=$db user=$user password=$pass sslmode=disable");
        if($conn)
        {
            $sql = "SELECT ogc_fid, date, latitude, longitude";
            if( isset($lat) && isset($lon) ){
                $sql = $sql . ", round(ST_Distance_Sphere(ST_PointFromText('POINT($lon $lat)', 4326), $table.wkb_geometry)) AS dist ";
            }

            $sql = $sql . " FROM $table";
            
            if( isset($lat) && isset($lon) ){
                $minx = $lon - $adds_degree;
                $miny = $lat - $adds_degree;
                $maxx = $lon + $adds_degree;
                $maxy = $lat + $adds_degree; 
                
                $sql = $sql . " WHERE ST_Intersects($table.wkb_geometry, ST_GeomFromText('POLYGON(($minx $maxy, $maxx $maxy, $maxx $miny, $minx $miny, $minx $maxy))', 4326) )";         

                if( isset($date) ){
                    $sql = $sql . " AND CAST(date as date) >= '$date'";
                }                   
            }
            else if( isset($date) ){
                $sql = $sql . " WHERE CAST(date as date) >= '$date'";
            }
            
            if( isset($radius) ){
                $sql = "SELECT * FROM ($sql)t WHERE dist <= $radius";
            }

            $sql = $sql . " LIMIT $limit";

            $result = pg_query($conn, $sql);
            if (!$result) {
                $return['error'] = true;
                $return['msg'] = "Error in SQL query: " . pg_last_error() . ". SQL: $sql";
                echo json_encode($return);
                return;
            }

            while ($row = pg_fetch_array($result)) {
                $outrow['fid'] = intval($row[0]);
                $outrow['date'] = $row[1];
                $outrow['lat'] = doubleval($row[2]);
                $outrow['lon'] = doubleval($row[3]);
                if(isset($lat) && isset($lon)){
                    $outrow['dist'] = doubleval($row[4]);
                }
                else {
                    $outrow['dist'] = -1; 
                }
                $rows[] = $outrow;
            }       

            pg_free_result($result); 

            $return['error'] = false;
            $return['rows'] = $rows;
            echo json_encode($return);
            pg_close($conn);
            return;
        }

        $return['error'] = true;
        $return['msg'] = 'connection failed';
        echo json_encode($return);
    }
    catch (Exception $e) 
    {
        $return['error'] = true;
        $return['msg'] = 'connection failed';
        echo json_encode($return);
    }
}


function get_rows_user($db, $user, $pass, $table, $lat, $lon, $radius, $date, $limit){
    if($limit > 50){
        $limit = 50;
    }
    $host = 'localhost';
    $port = 5432;
    $adds_degree = 5;

    try 
    {
        $conn = pg_connect("host=$host port=$port dbname=$db user=$user password=$pass sslmode=disable");
        if($conn)
        {
            $sql = "SELECT id, report_date, latitude, longitude";
            if( isset($lat) && isset($lon) ){
                $sql = $sql . ", round(ST_Distance_Sphere(ST_PointFromText('POINT($lon $lat)', 4326), $table.geom)) AS dist ";
            }

            $sql = $sql . " FROM $table";
            
            if( isset($lat) && isset($lon) ){
                $minx = $lon - $adds_degree;
                $miny = $lat - $adds_degree;
                $maxx = $lon + $adds_degree;
                $maxy = $lat + $adds_degree; 
                
                $sql = $sql . " WHERE ST_Intersects($table.geom, ST_GeomFromText('POLYGON(($minx $maxy, $maxx $maxy, $maxx $miny, $minx $miny, $minx $maxy))', 4326) )";       

                if( isset($date) ){
                    $sql = $sql . " AND CAST(report_date as date) >= '$date'";
                }   
            }
            else if( isset($date) ){
                $sql = $sql . " WHERE CAST(report_date as date) >= '$date'";
            }   
            
            if( isset($radius) ){
                $sql = "SELECT * FROM ($sql)t WHERE dist <= $radius";
            }

            $sql = $sql . " LIMIT $limit";

            $result = pg_query($conn, $sql);
            if (!$result) {
                $return['error'] = true;
                $return['msg'] = "Error in SQL query: " . pg_last_error() . ". SQL: $sql";
                echo json_encode($return);
                return;
            }

            while ($row = pg_fetch_array($result)) {
                $outrow['fid'] = intval($row[0]);
                $outrow['date'] = $row[1];
                $outrow['lat'] = doubleval($row[2]);
                $outrow['lon'] = doubleval($row[3]);
                if(isset($lat) && isset($lon)){
                    $outrow['dist'] = doubleval($row[4]);
                }
                else {
                    $outrow['dist'] = -1; 
                }
                $rows[] = $outrow;
            }       

            pg_free_result($result); 

            $return['error'] = false;
            $return['rows'] = $rows;
            echo json_encode($return);
            pg_close($conn);
            return;
        }

        $return['error'] = true;
        $return['msg'] = 'connection failed';
        echo json_encode($return);
    }
    catch (Exception $e) 
    {
        $return['error'] = true;
        $return['msg'] = 'connection failed';
        echo json_encode($return);
    }
}

function store_row($db, $user, $pass, $table, $lat, $lon, $date, $dist, $az, $comment){
    $host = 'localhost';
    $port = 5432;

    if(!isset($dist))
        $dist = 0;
    if(!isset($az))
        $az = 0; 
    if(!isset($comment))
        $comment = ''; 
    if(!isset($date))
        $date = date("Y-m-d H:i:s");
    try 
    {
        $conn = pg_connect("host=$host port=$port dbname=$db user=$user password=$pass sslmode=disable");
        if($conn)
        {
            $query = "INSERT INTO $table (geom, latitude, longitude, azimuth, distance, report_date, comment) VALUES (ST_GeomFromText('POINT($lon $lat)', 4326), $lat, $lon, $az, $dist, '$date', '$comment')";
            $result = pg_query($query);

            if($result)
            {
                $return['error'] = false;
                $return['msg'] = "row added successfully";
                echo json_encode($return);
                pg_free_result($result);
            }
            else
            {
                $return['error'] = true;
                $return['msg'] = "Error in SQL query: " . pg_last_error();
                echo json_encode($return);
            }

            pg_close($conn);
            return;
        }

        $return['error'] = true;
        $return['msg'] = 'connection failed';
        echo json_encode($return);
    }
    catch (Exception $e) 
    {
        $return['error'] = true;
        $return['msg'] = 'connection failed';
        echo json_encode($return);
    }
}

function test_conn($db, $user, $pass){
  $host = 'localhost';
  $port = 5432; 

  try 
  {
    $conn = pg_connect("host=$host port=$port dbname=$db user=$user password=$pass sslmode=disable");
    if($conn)
    {
        $return['error'] = false;
        $return['msg'] = 'connection succeeded';
        echo json_encode($return);
        pg_close($conn);
        return;
    }
    $return['error'] = true;
    $return['msg'] = 'connection failed';
    echo json_encode($return);
  }
  catch (Exception $e) 
  {
      $return['error'] = true;
      $return['msg'] = 'connection failed';
      echo json_encode($return);
  }
}
?>
