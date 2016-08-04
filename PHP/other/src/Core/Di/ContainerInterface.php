<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di;

/**
 * Interface ContainerInterface
 */
interface ContainerInterface
{
    /**
     * Get instantiate an object of class
     *
     * @param string $className
     * @return mixed
     */
    public function get($className);

    /**
     * Create new object
     *
     * @param string $className
     * @param array $arguments
     * @return mixed
     */
    public function create($className, array $arguments = []);
}
